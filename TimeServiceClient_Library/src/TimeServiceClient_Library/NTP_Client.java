package TimeServiceClient_Library;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;

import java.util.ArrayList;
import java.util.Collections;

import static java.lang.Thread.sleep;

public class NTP_Client {
    private static int NTP_Port = 123;
    private static int NTP_PACKET_SIZE = 48;
    private static long SeventyYears = 2208988800L;

    DatagramSocket m_TimeService_Socket;
    InetAddress m_TimeService_IPAddress;
    Boolean m_bNTP_Client_Started;

    public enum NTP_Client_ResultCode {NTP_Success, NTP_ServerAddressNotSet, NTP_SendFailed, NTP_ReceiveFailed}

    public final class NTP_Timestamp_Data {
        public NTP_Client_ResultCode eResultCode;
        public long lUnixTime;  // 恢复 Unix 时间
        public long lHour;
        public long lMinute;
        public long lSecond;
        public long offset;
        public long delay;
        public long t1, t2, t3, t4;

        NTP_Timestamp_Data() {
            eResultCode = NTP_Client_ResultCode.NTP_ServerAddressNotSet;
            lHour = 0;
            lMinute = 0;
            lSecond = 0;
            offset = 0;
            delay = 0;
            lUnixTime = 0;
            t1 = t2 = t3 = t4 = 0;
        }
    }

    private Boolean m_bTimeServiceAddressSet;

    public NTP_Client() {
        m_bTimeServiceAddressSet = false;
        m_bNTP_Client_Started = false;
    }

    public Boolean CreateSocket() {
        try {
            m_TimeService_Socket = new DatagramSocket();
            m_TimeService_Socket.setSoTimeout(500);
        } catch (SocketException Ex) {
            return false;
        }
        return true;
    }

    public InetAddress SetUp_TimeService_AddressStruct(String sURL) {
        String sFullURL = "http://" + sURL;
        try {
            m_TimeService_IPAddress = InetAddress.getByName(new URL(sFullURL).getHost());
            m_bTimeServiceAddressSet = true;
        } catch (Exception Ex) {
            return null;
        }
        return m_TimeService_IPAddress;
    }

    public int GetPort() {
        return NTP_Port;
    }

    public NTP_Timestamp_Data Get_Averaged_NTP_Timestamp() throws InterruptedException {
        NTP_Timestamp_Data averagedTimestamp = new NTP_Timestamp_Data();
        ArrayList<Long> offsets = new ArrayList<>();
        ArrayList<Long> delays = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            NTP_Timestamp_Data NTP_Timestamp = Get_NTP_Timestamp();
            averagedTimestamp.eResultCode = NTP_Timestamp.eResultCode;

            if (NTP_Timestamp.lUnixTime == 0 || NTP_Timestamp.t1 == 0) {
                i--;  // 如果数据无效，则重试
                continue;
            }

            averagedTimestamp.lUnixTime = NTP_Timestamp.lUnixTime;
            averagedTimestamp.lHour = NTP_Timestamp.lHour;
            averagedTimestamp.lMinute = NTP_Timestamp.lMinute;
            averagedTimestamp.lSecond = NTP_Timestamp.lSecond;

            // 输出每次请求的详细信息
            System.out.println("Request " + (i + 1) + ":");
            System.out.println("t1: " + NTP_Timestamp.t1);
            System.out.println("t2: " + NTP_Timestamp.t2);
            System.out.println("t3: " + NTP_Timestamp.t3);
            System.out.println("t4: " + NTP_Timestamp.t4);
            System.out.println("Unix Time: " + NTP_Timestamp.lUnixTime);
            System.out.printf("Time: %02d:%02d:%02d\n", NTP_Timestamp.lHour, NTP_Timestamp.lMinute, NTP_Timestamp.lSecond);
            System.out.println("Offset: " + NTP_Timestamp.offset);
            System.out.println("Delay: " + NTP_Timestamp.delay);
            System.out.println();

            offsets.add(NTP_Timestamp.offset);
            delays.add(NTP_Timestamp.delay);

            if (i < 9) {
                Thread.sleep(2000); // 等待5秒后再发送下一个请求
            }
        }

        // 去除最大值和最小值
        Collections.sort(offsets);
        Collections.sort(delays);
        offsets.remove(0);
        offsets.remove(offsets.size() - 1);
        delays.remove(0);
        delays.remove(delays.size() - 1);

        // **新增代码：计算中位数并剔除偏离中位数较大的值**
        ArrayList<Long> filteredOffsets = new ArrayList<>(offsets);
        ArrayList<Long> filteredDelays = new ArrayList<>(delays);
        for(int i = 0; i < offsets.size(); i++) {
            System.out.println("Offset " + (i + 1) + ": " + offsets.get(i));
        }
        long medianOffset = filteredOffsets.get(filteredOffsets.size() / 2);
        long medianDelay = filteredDelays.get(filteredDelays.size() / 2);

        // 移除偏离中位数超过30%的数据
        filteredOffsets.removeIf(offset -> Math.abs(offset - medianOffset) > Math.abs(medianOffset) * 1);
        filteredDelays.removeIf(delay -> Math.abs(delay - medianDelay) > medianDelay * 1);

        // **计算剩余值的平均值**
        long totalOffset = 0, totalDelay = 0;
        for (long offset : filteredOffsets) {
            totalOffset += offset;
        }
        for (long delay : filteredDelays) {
            totalDelay += delay;
        }
        for(int i = 0; i < filteredOffsets.size(); i++) {
            System.out.println("Filtered Offset " + (i + 1) + ": " + filteredOffsets.get(i));
        }
        for(int i = 0; i < filteredDelays.size(); i++) {
            System.out.println("Filtered Delay " + (i + 1) + ": " + filteredDelays.get(i));
        }
        averagedTimestamp.offset = filteredOffsets.isEmpty() ? 0 : totalOffset / filteredOffsets.size();
        averagedTimestamp.delay = filteredDelays.isEmpty() ? 0 : totalDelay / filteredDelays.size();

        // 根据平均的 offset 调整本地时钟
//        averagedTimestamp.lUnixTime += averagedTimestamp.offset / 1000;
//        averagedTimestamp.lSecond = averagedTimestamp.lSecond + (averagedTimestamp.offset / 1000) % 60;
        return averagedTimestamp;
    }

    public Boolean Get_ClientStarted_Flag() {
        return m_bNTP_Client_Started;
    }

    public void Set_ClientStarted_Flag(Boolean bClient_Started) {
        m_bNTP_Client_Started = bClient_Started;
    }

    public NTP_Timestamp_Data Get_NTP_Timestamp() {
        NTP_Timestamp_Data NTP_Timestamp = new NTP_Timestamp_Data();
        if (m_bTimeServiceAddressSet) {
            NTP_Timestamp.t1 = System.currentTimeMillis();
            if (Send_TimeService_Request()) {
                NTP_Timestamp = Receive(NTP_Timestamp);
                if (NTP_Timestamp.t3 != 0 && NTP_Timestamp.t2 != 0) {
                    System.out.println("Receive Success");
                    NTP_Timestamp.eResultCode = NTP_Client_ResultCode.NTP_Success;

                    return NTP_Timestamp;
                }
                NTP_Timestamp.eResultCode = NTP_Client_ResultCode.NTP_ReceiveFailed;
                return NTP_Timestamp;
            }
            NTP_Timestamp.eResultCode = NTP_Client_ResultCode.NTP_SendFailed;
            return NTP_Timestamp;
        }
        NTP_Timestamp.eResultCode = NTP_Client_ResultCode.NTP_ServerAddressNotSet;
        return NTP_Timestamp;
    }

    Boolean Send_TimeService_Request() {
        byte[] bSendBuf = new byte[NTP_PACKET_SIZE];
        bSendBuf[0] = (byte) 0xE3;

        try {
            DatagramPacket SendPacket = new DatagramPacket(bSendBuf, bSendBuf.length,
                    m_TimeService_IPAddress, NTP_Port);
            m_TimeService_Socket.send(SendPacket);
        } catch (Exception Ex) {
            return false;
        }
        return true;
    }

    private NTP_Timestamp_Data Receive(NTP_Timestamp_Data NTP_Timestamp) {
        byte[] bRecvBuf = new byte[NTP_PACKET_SIZE];
        DatagramPacket RecvPacket = new DatagramPacket(bRecvBuf, NTP_PACKET_SIZE);
        try {
            m_TimeService_Socket.receive(RecvPacket);
            NTP_Timestamp.t4 = System.currentTimeMillis();
        } catch (Exception ex) {
            return NTP_Timestamp;
        }

        if (RecvPacket.getLength() > 0) {
            long l1 = (long) bRecvBuf[32] & 0xFF;
            long l2 = (long) bRecvBuf[33] & 0xFF;
            long l3 = (long) bRecvBuf[34] & 0xFF;
            long l4 = (long) bRecvBuf[35] & 0xFF;
            NTP_Timestamp.t2 = ((l1 << 24) + (l2 << 16) + (l3 << 8) + l4) * 1000L - SeventyYears * 1000L;

            l1 = (long) bRecvBuf[40] & 0xFF;
            l2 = (long) bRecvBuf[41] & 0xFF;
            l3 = (long) bRecvBuf[42] & 0xFF;
            l4 = (long) bRecvBuf[43] & 0xFF;

            long secsSince1900 = (l1 << 24) + (l2 << 16) + (l3 << 8) + l4;
            NTP_Timestamp.lUnixTime = secsSince1900 - SeventyYears;    // Subtract seventy years
            NTP_Timestamp.lHour = (long) (NTP_Timestamp.lUnixTime % 86400L) / 3600;
            NTP_Timestamp.lMinute = (long) (NTP_Timestamp.lUnixTime % 3600) / 60;
            NTP_Timestamp.lSecond = (long) NTP_Timestamp.lUnixTime % 60;

            NTP_Timestamp.t3 = ((l1 << 24) + (l2 << 16) + (l3 << 8) + l4) * 1000L - SeventyYears * 1000L;

            NTP_Timestamp.delay = (NTP_Timestamp.t4 - NTP_Timestamp.t1) - (NTP_Timestamp.t3 - NTP_Timestamp.t2);
            NTP_Timestamp.offset = ((NTP_Timestamp.t2 - NTP_Timestamp.t1) + (NTP_Timestamp.t3 - NTP_Timestamp.t4)) / 2;
        }
        return NTP_Timestamp;
    }

    public void CloseSocket() {
        try {
            m_TimeService_Socket.close();
        } catch (Exception Ex) {   // Generic approach to dealing with situations such as socket not created
        }
    }

    public static void main(String[] args) throws InterruptedException {
        NTP_Client ntp = new NTP_Client();
        ntp.CreateSocket();
        ntp.SetUp_TimeService_AddressStruct("time.windows.com");

        NTP_Client.NTP_Timestamp_Data data = ntp.Get_Averaged_NTP_Timestamp();

        // 打印输出
        System.out.println("Averaged Offset: " + data.offset);
        System.out.println("Averaged Delay: " + data.delay);
        System.out.println("lUnixTime: " + data.lUnixTime);
        System.out.printf("Time: %02d:%02d:%02d\n", data.lHour, data.lMinute, data.lSecond);

        ntp.m_TimeService_Socket.close();
    }
}
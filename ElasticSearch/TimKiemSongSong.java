import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class TimKiemSongSong {

    private static final String THU_MUC_LOG = "D:\\LogData\\logs";        // Thư mục chứa 3000 file log
    private static final String THU_MUC_KET_QUA = "D:\\LogData\\KetQua"; // Thư mục chứa các file kết quả
    private static final String FILE_TONG_HOP = "D:\\LogData\\KetQua\\tonghop.txt"; // File thống kê
    private static final String TU_KHOA = "login by 99";                 // Từ khóa cần tìm

    public static void main(String[] args) {
        File folderLog = new File(THU_MUC_LOG);
        File[] files = folderLog.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            System.out.println("❌ Không tìm thấy file log trong thư mục: " + THU_MUC_LOG);
            return;
        }

        // Tạo (hoặc làm sạch) thư mục kết quả
        File folderKQ = new File(THU_MUC_KET_QUA);
        if (!folderKQ.exists()) {
            folderKQ.mkdirs();
        } else {
            File[] old = folderKQ.listFiles();
            if (old != null) {
                for (File f : old) {
                    f.delete(); // xóa các file kết quả cũ
                }
            }
        }

        int soLuongThread = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(soLuongThread);

        System.out.println("🔍 Đang tìm kiếm song song bằng " + soLuongThread + " luồng...");

        List<Future<Integer>> futures = new ArrayList<>();

        // Giao mỗi file cho 1 task: trả về số kết quả tìm thấy trong file đó
        for (File file : files) {
            futures.add(executor.submit(() -> timTrongFile(file)));
        }

        executor.shutdown();

        // Ghi file thống kê tổng hợp
        int tongKetQua = 0;
        try (BufferedWriter writerTongHop = new BufferedWriter(new FileWriter(FILE_TONG_HOP))) {

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                int soKQ = 0;
                try {
                    soKQ = futures.get(i).get(); // chờ task xong và lấy số kết quả
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("⚠️ Luồng bị gián đoạn khi chờ kết quả.");
                } catch (ExecutionException e) {
                    System.out.println("⚠️ Lỗi trong khi xử lý file: " + file.getName() + " - " + e.getCause());
                }

                tongKetQua += soKQ;

                if (soKQ > 0) {
                    writerTongHop.write(String.format("File: %-30s  ->  %d dòng khớp", file.getName(), soKQ));
                    writerTongHop.newLine();
                }
            }

            writerTongHop.write("==============================================");
            writerTongHop.newLine();
            writerTongHop.write("TỔNG SỐ DÒNG KHỚP TẤT CẢ CÁC FILE: " + tongKetQua);
            writerTongHop.newLine();

        } catch (IOException e) {
            System.out.println("❌ Không thể ghi file thống kê tonghop.txt: " + e.getMessage());
        }

        System.out.println("✅ Hoàn tất tìm kiếm.");
        System.out.println("📁 Các file kết quả nằm trong thư mục: " + THU_MUC_KET_QUA);
        System.out.println("📄 Thống kê tổng thể nằm trong: " + FILE_TONG_HOP);
    }

    /**
     * Tìm trong 1 file log:
     *  - Ghi TẤT CẢ các dòng chứa từ khóa vào file riêng: <tenfile>_ketqua.txt
     *  - Trả về tổng số dòng khớp tìm thấy.
     */
    private static int timTrongFile(File file) {
        int soKetQua = 0;
        StringBuilder buffer = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String dong;
            int dongSo = 0;

            while ((dong = br.readLine()) != null) {
                dongSo++;
                if (dong.contains(TU_KHOA)) {
                    soKetQua++;
                    buffer.append("Dòng: ")
                            .append(dongSo)
                            .append(" - Nội dung: ")
                            .append(dong)
                            .append(System.lineSeparator());
                }
            }

        } catch (IOException e) {
            System.out.println("⚠️ Lỗi khi đọc file: " + file.getName() + " - " + e.getMessage());
        }

        // Nếu có kết quả thì ghi ra file riêng cho log này
        if (soKetQua > 0) {
            File outFile = new File(THU_MUC_KET_QUA, file.getName().replace(".txt", "") + "_ketqua.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
                writer.write(buffer.toString());
            } catch (IOException e) {
                System.out.println("⚠️ Lỗi khi ghi file kết quả cho: " + file.getName() + " - " + e.getMessage());
            }
        }

        return soKetQua;
    }
}

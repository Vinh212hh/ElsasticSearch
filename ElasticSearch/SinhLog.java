import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SinhLog {
    public static void main(String[] args) {
        File folder = new File("D:\\LogData\\logs");
        if (!folder.exists()) folder.mkdirs();

        int soFile = 3000;
        int soDongMoiFile = 20000;
        String[] suKien = {
                "user login by 10", "user logout", "server started",
                "database connected", "error 404", "login by 99", "system idle"
        };

        Random rand = new Random();
        DecimalFormat df = new DecimalFormat("00");
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yy");

        System.out.println("Đang tạo file log...");

        for (int i = 1; i <= soFile; i++) {
            String tenFile = "log_" + df.format(i) + "_11_25.txt";
            File f = new File(folder, tenFile);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
                for (int j = 1; j <= soDongMoiFile; j++) {
                    String dong = "[" + new Date() + "] " + suKien[rand.nextInt(suKien.length)];
                    bw.write(dong);
                    bw.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (i % 100 == 0) {
                System.out.println("Đã tạo " + i + "/" + soFile + " file...");
            }
        }

        System.out.println("Hoàn tất tạo 3000 file log tại thư mục: " + folder.getAbsolutePath());
    }
}


import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchGui extends JFrame {

    private JTextField txtFindWhat;
    private JTextField txtDirectory;
    private JCheckBox chkWholeWord;
    private JCheckBox chkMatchCase;
    private JTextArea txtResult;
    private JButton btnFindAll;

    private DefaultListModel<FileHit> listModel;
    private JList<FileHit> lstFiles;

    // Giới hạn ký tự hiển thị để UI không lag (có thể chỉnh nhỏ hơn nữa)
    private static final int MAX_RESULT_CHARS = 200_000;
    private static final int MAX_FILE_VIEW_CHARS = 2_000_000;

    private Pattern currentPattern;
    private volatile boolean resultLimitReached = false;

    public SearchGui() {
        setTitle("Find in Files");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 500);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        // ======= TOP PANEL =======
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        // Find what
        gc.gridx = 0;
        topPanel.add(new JLabel("Find what :"), gc);

        txtFindWhat = new JTextField(30);
        gc.gridx = 1;
        gc.weightx = 1.0;
        topPanel.add(txtFindWhat, gc);

        btnFindAll = new JButton("Find All");
        gc.gridx = 2;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.EAST;
        topPanel.add(btnFindAll, gc);

        // Directory
        gc.gridy = 1;
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        topPanel.add(new JLabel("Directory :"), gc);

        txtDirectory = new JTextField(40);
        txtDirectory.setText("D:\\");
        gc.gridx = 1;
        gc.weightx = 1.0;
        topPanel.add(txtDirectory, gc);

        JButton btnBrowse = new JButton("...");
        gc.gridx = 2;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        topPanel.add(btnBrowse, gc);

        // Checkboxes
        JPanel chkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chkWholeWord = new JCheckBox("Match whole word only");
        chkMatchCase = new JCheckBox("Match case");
        chkPanel.add(chkWholeWord);
        chkPanel.add(chkMatchCase);

        gc.gridy = 2;
        gc.gridx = 1;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.NONE;
        topPanel.add(chkPanel, gc);

        add(topPanel, BorderLayout.NORTH);

        // ======= CENTER (list file + kết quả) =======
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JLabel lblTitle = new JLabel("Kết quả", SwingConstants.CENTER);
        centerPanel.add(lblTitle, BorderLayout.NORTH);

        // list file
        listModel = new DefaultListModel<>();
        lstFiles = new JList<>(listModel);
        lstFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollList = new JScrollPane(lstFiles);
        scrollList.setBorder(BorderFactory.createTitledBorder("File chứa kết quả"));

        // text area kết quả chi tiết
        txtResult = new JTextArea();
        txtResult.setEditable(false);
        txtResult.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollText = new JScrollPane(txtResult);
        scrollText.setBorder(BorderFactory.createTitledBorder("Chi tiết kết quả (file - dòng - nội dung)"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollList, scrollText);
        split.setResizeWeight(0.3);
        centerPanel.add(split, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // ======= EVENTS =======
        btnFindAll.addActionListener(this::onFindAll);
        txtFindWhat.addActionListener(this::onFindAll);
        btnBrowse.addActionListener(e -> chooseDirectory());

        lstFiles.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FileHit hit = lstFiles.getSelectedValue();
                    if (hit != null) {
                        openFileViewer(hit.file);
                    }
                }
            }
        });
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        File currentDir = new File(txtDirectory.getText().trim());
        if (currentDir.exists()) {
            chooser.setCurrentDirectory(currentDir);
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            txtDirectory.setText(dir.getAbsolutePath());
        }
    }

    private void onFindAll(ActionEvent e) {
        String keyword = txtFindWhat.getText().trim();
        String dirPath = txtDirectory.getText().trim();

        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập chuỗi cần tìm (Find what).");
            return;
        }

        if (dirPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thư mục (Directory).");
            return;
        }

        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Thư mục không hợp lệ: " + dirPath);
            return;
        }

        // reset kết quả cũ
        txtResult.setText("Đang tìm kiếm...\n");
        listModel.clear();
        resultLimitReached = false;

        boolean wholeWord = chkWholeWord.isSelected();
        boolean matchCase = chkMatchCase.isSelected();

        String regex = Pattern.quote(keyword);
        if (wholeWord) {
            regex = "\\b" + regex + "\\b";
        }

        int flags = Pattern.MULTILINE;
        if (!matchCase) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        Pattern pattern = Pattern.compile(regex, flags);
        currentPattern = pattern;

        new Thread(() -> searchInDirectory(dir, pattern)).start();
    }

    // ========== GHI KẾT QUẢ VÀO TEXTAREA THEO BLOCK ==========
    private void appendResultLine(String text) {
        appendResultBlock(text);
    }

    private void appendResultBlock(String text) {
        if (resultLimitReached) return;

        SwingUtilities.invokeLater(() -> {
            if (txtResult.getDocument().getLength() > MAX_RESULT_CHARS) {
                if (!resultLimitReached) {
                    resultLimitReached = true;
                    txtResult.append("\n--- Chỉ hiển thị một phần, mở file để xem đầy đủ ---\n");
                }
                return;
            }
            txtResult.append(text);
            txtResult.setCaretPosition(txtResult.getDocument().getLength());
        });
    }

    private void addFileToList(File file, int matchCount) {
        SwingUtilities.invokeLater(() -> {
            listModel.addElement(new FileHit(file, matchCount));
        });
    }

    private void searchInDirectory(File dir, Pattern pattern) {
        appendResultLine("Đang quét thư mục: " + dir.getAbsolutePath() + "\n");
        searchRecursive(dir, pattern);
        appendResultLine("\nHoàn tất.\n");
    }

    private void searchRecursive(File file, Pattern pattern) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    searchRecursive(child, pattern);
                }
            }
        } else {
            String name = file.getName().toLowerCase();
            if (!(name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".csv"))) {
                return;
            }
            searchInFile(file, pattern);
        }
    }

    // ========== TÌM TRONG 1 FILE – GOM KẾT QUẢ THEO FILE ==========
    private void searchInFile(File file, Pattern pattern) {
        int matchCount = 0;
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    matchCount++;
                    sb.append(file.getName())
                            .append(" - dòng ").append(lineNumber)
                            .append(" - ").append(line)
                            .append("\n");
                }
            }
        } catch (IOException ex) {
            appendResultLine("Lỗi khi đọc file: " + file.getAbsolutePath()
                    + " -> " + ex.getMessage() + "\n");
        }

        if (matchCount > 0) {
            appendResultBlock(sb.toString()); // chỉ append 1 lần / file
            addFileToList(file, matchCount);
        }
    }

    // ====== MỞ CỬA SỔ XEM FILE + HIGHLIGHT ======
    private void openFileViewer(File file) {
        JFrame frame = new JFrame("Xem file: " + file.getName());
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(this);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(area);
        frame.add(scrollPane, BorderLayout.CENTER);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                if (sb.length() > MAX_FILE_VIEW_CHARS) {
                    sb.append("\n--- Bị cắt bớt do file quá lớn, nhưng vẫn đủ để xem quanh vị trí match ---");
                    break;
                }
                sb.append(line).append("\n");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Không thể đọc file: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        area.setText(sb.toString());
        area.setCaretPosition(0);
        highlightInTextArea(area);

        frame.setVisible(true);
    }

    private void highlightInTextArea(JTextArea area) {
        if (currentPattern == null) return;

        String text = area.getText();
        Matcher matcher = currentPattern.matcher(text);

        Highlighter highlighter = area.getHighlighter();
        highlighter.removeAllHighlights();
        Highlighter.HighlightPainter painter =
                new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        int firstPos = -1;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            try {
                highlighter.addHighlight(start, end, painter);
            } catch (BadLocationException ignored) {}
            if (firstPos == -1) firstPos = start;
        }

        if (firstPos != -1) {
            area.setCaretPosition(firstPos);
        }
    }

    private static class FileHit {
        final File file;
        final int count;

        FileHit(File file, int count) {
            this.file = file;
            this.count = count;
        }

        @Override
        public String toString() {
            return file.getName() + " (" + count + " kết quả)";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SearchGui().setVisible(true));
    }
}

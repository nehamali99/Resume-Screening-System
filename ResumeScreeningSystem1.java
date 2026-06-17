package mypack;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class ResumeScreeningSystem1 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginFrame::new);
    }

    // ================= LOGIN =================
    static class LoginFrame extends JFrame {
        JTextField user;
        JPasswordField pass;

        LoginFrame() {
            setTitle("Resume Screening Login");
            setSize(420, 300);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            JPanel p = new JPanel();
            p.setLayout(null);
            p.setBackground(new Color(245, 248, 255));

            JLabel t = new JLabel("STUDENT LOGIN");
            t.setBounds(120, 20, 200, 30);
            t.setForeground(new Color(30, 90, 160));
            t.setFont(new Font("Arial", Font.BOLD, 18));

            user = new JTextField();
            pass = new JPasswordField();

            JLabel u = new JLabel("Username:");
            JLabel pw = new JLabel("Password:");

            u.setBounds(50, 80, 100, 25);
            pw.setBounds(50, 120, 100, 25);

            user.setBounds(150, 80, 200, 25);
            pass.setBounds(150, 120, 200, 25);

            JButton btn = new JButton("LOGIN");
            btn.setBounds(150, 170, 120, 35);
            btn.setBackground(new Color(70, 130, 230));
            btn.setForeground(Color.WHITE);

            btn.addActionListener(e -> {
                if (user.getText().equals("student") &&
                        new String(pass.getPassword()).equals("1234")) {
                    dispose();
                    new MainFrame();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Login");
                }
            });

            p.add(t);
            p.add(u);
            p.add(pw);
            p.add(user);
            p.add(pass);
            p.add(btn);

            add(p);
            setVisible(true);
        }
    }

    // ================= MAIN UI =================
    static class MainFrame extends JFrame {

        JTextArea keywordArea = new JTextArea();
        JTextArea resumeArea = new JTextArea();
        JTextArea aiOutput = new JTextArea();

        DefaultTableModel model;
        java.util.List<Candidate> list = new ArrayList<>();

        MainFrame() {

            setTitle("Resume Screening System (AI + PDF)");
            setSize(950, 650);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            JPanel p = new JPanel();
            p.setLayout(null);
            p.setBackground(new Color(250, 250, 250));

            JLabel title = new JLabel("AI RESUME SCREENING SYSTEM");
            title.setBounds(280, 10, 400, 30);
            title.setFont(new Font("Arial", Font.BOLD, 20));
            title.setForeground(new Color(20, 90, 170));

            JButton upload = new JButton("Upload PDF Resume");
            JButton score = new JButton("Generate Score");
            JButton ai = new JButton("AI Analysis (Gemini)");
            JButton pdf = new JButton("Export PDF Report");

            upload.setBounds(30, 60, 180, 35);
            score.setBounds(220, 60, 160, 35);
            ai.setBounds(390, 60, 180, 35);
            pdf.setBounds(580, 60, 180, 35);

            JTextArea keywordArea = this.keywordArea;
            keywordArea.setBounds(30, 140, 300, 80);

            resumeArea.setBounds(350, 140, 350, 80);
            aiOutput.setBounds(720, 140, 200, 80);

            model = new DefaultTableModel(new String[]{"Name", "Score"}, 0);
            JTable table = new JTable(model);
            JScrollPane sp = new JScrollPane(table);
            sp.setBounds(30, 260, 880, 300);

            upload.setBackground(new Color(220, 235, 255));
            score.setBackground(new Color(210, 255, 220));
            ai.setBackground(new Color(255, 245, 200));
            pdf.setBackground(new Color(255, 220, 220));

            // ========== UPLOAD PDF ==========
            upload.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    String text = extractPDF(f);
                    resumeArea.setText(text);
                    list.add(new Candidate(f.getName(), text));
                }
            });

            // ========== SCORE ==========
            score.addActionListener(e -> {

                String[] keys = keywordArea.getText().toLowerCase().split(",");

                model.setRowCount(0); // Clear old rows

                for (Candidate c : list) {

                    int s = 0;

                    for (String k : keys) {
                        if (c.text.toLowerCase().contains(k.trim())) {
                            s += 10;
                        }
                    }

                    c.score = s;

                    // Add candidate to table
                    model.addRow(new Object[]{
                            c.name,
                            c.score
                    });
                }

                JOptionPane.showMessageDialog(this,
                        "Score Generated Successfully!");
            });

            // ========== AI GEMINI ==========
            ai.addActionListener(e -> {
                if (list.isEmpty()) return;

                String result = callGemini(list.get(0).text);
                aiOutput.setText(result);
            });

            // ========== EXPORT PDF REPORT ==========
            pdf.addActionListener(e -> exportPDF());

            p.add(title);
            p.add(upload);
            p.add(score);
            p.add(ai);
            p.add(pdf);

            JLabel k = new JLabel("Keywords:");
            k.setBounds(30, 120, 100, 20);

            JLabel r = new JLabel("Resume:");
            r.setBounds(350, 120, 100, 20);

            JLabel a = new JLabel("AI Output:");
            a.setBounds(720, 120, 100, 20);

            p.add(k);
            p.add(r);
            p.add(a);

            p.add(keywordArea);
            p.add(resumeArea);
            p.add(aiOutput);
            p.add(sp);

            add(p);
            setVisible(true);
        }

        // ================= PDF PARSE =================
        String extractPDF(File file) {
            try (PDDocument doc = PDDocument.load(file)) {
                PDFTextStripper strip = new PDFTextStripper();
                return strip.getText(doc);
            } catch (Exception e) {
                return "PDF Read Error";
            }
        }

        // ================= GEMINI API =================
     // ================= OFFLINE AI ANALYSIS =================
        String callGemini(String resumeText) {

            resumeText = resumeText.toLowerCase();

            int score = 0;
            StringBuilder skills = new StringBuilder();

            String[] keywords = {
                    "java", "python", "sql", "html", "css",
                    "javascript", "spring", "mysql",
                    "machine learning", "data analysis",
                    "react", "git", "aws"
            };

            for (String skill : keywords) {
                if (resumeText.contains(skill)) {
                    score += 8;
                    skills.append(skill).append(", ");
                }
            }

            String level;

            if (score >= 70)
                level = "Excellent";
            else if (score >= 40)
                level = "Good";
            else
                level = "Needs Improvement";

            return "===== AI RESUME ANALYSIS =====\n\n"
                    + "Resume Score : " + score + "/100"
                    + "\n\nSkills Found:\n"
                    + skills
                    + "\n\nOverall Rating : " + level
                    + "\n\nRecommendation : Suitable for screening round.";
        }

        // ================= EXPORT PDF =================
        void exportPDF() {
            try (PDDocument doc = new PDDocument()) {

                PDPage page = new PDPage();
                doc.addPage(page);

                PDPageContentStream cs = new PDPageContentStream(doc, page);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(50, 700);
                cs.showText("RESUME SCREENING REPORT");
                cs.endText();

                int y = 650;

                for (Candidate c : list) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, y);
                    cs.showText(c.name + " | Score: " + c.score);
                    cs.endText();
                    y -= 20;
                }

                cs.close();

                doc.save("Resume_Report.pdf");
                JOptionPane.showMessageDialog(this, "PDF Report Generated!");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ================= MODEL =================
    static class Candidate {
        String name;
        String text;
        int score;

        Candidate(String n, String t) {
            name = n;
            text = t;
        }
    }
}
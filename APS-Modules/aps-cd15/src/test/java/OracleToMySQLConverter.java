import java.io.*;
import java.util.Scanner;
import java.util.regex.*;

public class OracleToMySQLConverter {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 从控制台获取输入文件路径
        System.out.print("请输入输入文件路径: ");
        String inputFile = scanner.nextLine().trim();

        // 从控制台获取输出文件路径
        System.out.print("请输入输出文件路径: ");
        String outputFile = scanner.nextLine().trim();

        scanner.close();

        // 检查输入文件是否存在
        File input = new File(inputFile);
        if (!input.exists() || !input.isFile()) {
            System.err.println("输入文件不存在或路径无效: " + inputFile);
            return;
        }

        // 检查输出文件路径是否有效
        File output = new File(outputFile);
        if (output.exists()) {
            System.err.println("输出文件已存在，请指定一个新的文件路径: " + outputFile);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 应用转换规则
                line = convertOracleToMySQL(line);
                writer.write(line);
                writer.newLine();
            }

            System.out.println("转换完成！输出文件已保存到: " + outputFile);

        } catch (IOException e) {
            System.err.println("处理文件时发生错误: " + e.getMessage());
        }
    }

    private static String convertOracleToMySQL(String line) {
        line = line.replaceAll("\"", "");
        // 1. 替换SYSDATE为NOW()
        line = line.replaceAll("(?i)SYSDATE", "NOW()");

        // 2. 替换序列值为NULL（假设MySQL使用AUTO_INCREMENT）
        line = line.replaceAll("(?i)(\\w+)\\.NEXTVAL", "NULL");

        // 3. 转换TO_DATE函数
        line = convertTO_DATE(line);

        // 4. 替换VARCHAR2为VARCHAR
//        line = line.replaceAll("(?i)VARCHAR2", "VARCHAR");

        // 5. 替换NUMBER为DECIMAL（可选）
//        line = line.replaceAll("(?i)NUMBER\\(\\d+,\\d+\\)", "DECIMAL");
//        line = line.replaceAll("(?i)NUMBER", "INT");

        // 6. 处理分页语法（简单情况）
//        line = line.replaceAll("(?i)ROWNUM\\s*<=\\s*(\\d+)", "1 LIMIT $1");

        return line;
    }

    private static String convertTO_DATE(String line) {
        Pattern pattern = Pattern.compile("TO_DATE\\('(.*?)',\\s*'(.*?)'\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String dateValue = matcher.group(1);
            String oracleFormat = matcher.group(2);
            String mysqlFormat = convertDateFormat(oracleFormat);
            matcher.appendReplacement(sb, "STR_TO_DATE('" + dateValue + "', '" + mysqlFormat + "')");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertDateFormat(String oracleFormat) {
        return oracleFormat.toUpperCase()
            .replace("SYYYY", "%Y")
            .replace("YY", "%y")
            .replace("MM", "%m")
            .replace("DD", "%d")
            .replace("HH24", "%H")
            .replace("HH", "%h")
            .replace("MI", "%i")
            .replace("SS", "%s");
    }
}
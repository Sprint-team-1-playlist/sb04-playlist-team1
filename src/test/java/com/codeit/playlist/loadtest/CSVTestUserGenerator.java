package com.codeit.playlist.loadtest;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVTestUserGenerator {

  public static void main(String[] args) {
    String filePath = "test-users.csv"; // 생성될 CSV 파일
    String prefix = "tester";
    int count = 50;
    String password = "abcd1234?";

    List<String[]> rows = new ArrayList<>();

    for (int i = 1; i <= count; i++) {
      String email = prefix + i + "@test.com";
      rows.add(new String[]{email, password});
    }

    writeCsv(filePath, rows);
    System.out.println("CSV 생성 완료 → " + filePath);
  }

  private static void writeCsv(String filePath, List<String[]> rows) {
    try (FileWriter writer = new FileWriter(filePath)) {
      for (String[] row : rows) {
        writer.write(String.join(",", row));
        writer.write("\n");
      }
    } catch (IOException e) {
      System.err.println("CSV 생성 실패: " + e.getMessage());
    }
  }
}

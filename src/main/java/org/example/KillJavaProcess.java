package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class KillJavaProcess {
    public static void kill() {
        ProcessHandle processHandle = ProcessHandle.current();
        // Replace with the actual PID you want to kill
        long pid = processHandle.pid();

        // Get the operating system name
        String os = System.getProperty("os.name").toLowerCase();

        // Command to kill the process based on the operating system
        String killCommand;
        if (os.contains("win")) {
            // Windows command to kill a process
            killCommand = "taskkill /F /T /PID " + pid;
        } else {
            // Unix/Linux/Mac command to kill a process
            // killCommand = "kill -9 " + pid;
            killCommand = "pkill -TERM -P " + pid;
        }

        // Execute the kill command
        try {
            Process process = Runtime.getRuntime().exec(killCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();
            System.out.println("Process " + pid + " killed successfully.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

package com.subscriptionengine.fitnesse.server;

import fitnesseMain.FitNesseMain;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * FitNesse server wrapper
 * 
 * Manages the lifecycle of the FitNesse server
 */
@Slf4j
public class FitNesseServer {
    
    private final int port;
    private final String rootPath;
    private final String rootPagePath;
    private Thread fitNesseThread;
    
    public FitNesseServer(int port, String rootPath, String rootPagePath) {
        this.port = port;
        this.rootPath = rootPath;
        this.rootPagePath = rootPagePath;
    }
    
    public void start() throws Exception {
        log.info("Starting FitNesse server on port {}", port);
        
        // Ensure FitNesseRoot directory exists
        File rootDir = new File(rootPath);
        if (!rootDir.exists()) {
            log.info("Creating FitNesse root directory: {}", rootPath);
            rootDir.mkdirs();
        }
        
        // Start FitNesse in a separate thread
        fitNesseThread = new Thread(() -> {
            try {
                String[] args = {
                    "-p", String.valueOf(port),
                    "-d", ".",
                    "-e", "0",
                    "-o",
                    "-v"  // Verbose logging
                };
                
                log.info("Starting FitNesse with args: {}", String.join(" ", args));
                FitNesseMain.main(args);
                
            } catch (Exception e) {
                log.error("FitNesse server error", e);
                e.printStackTrace();
            }
        }, "FitNesse-Server");
        
        fitNesseThread.setDaemon(false);
        fitNesseThread.start();
        
        // Wait a bit for server to start
        Thread.sleep(2000);
        
        log.info("FitNesse server started successfully");
    }
    
    public void stop() {
        if (fitNesseThread != null && fitNesseThread.isAlive()) {
            log.info("Stopping FitNesse server");
            fitNesseThread.interrupt();
        }
    }
    
    public boolean isRunning() {
        return fitNesseThread != null && fitNesseThread.isAlive();
    }
}

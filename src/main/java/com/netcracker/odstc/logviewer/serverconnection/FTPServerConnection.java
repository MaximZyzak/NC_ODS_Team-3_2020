package com.netcracker.odstc.logviewer.serverconnection;

import com.netcracker.odstc.logviewer.containers.HierarchyContainer;
import com.netcracker.odstc.logviewer.models.Directory;
import com.netcracker.odstc.logviewer.models.Log;
import com.netcracker.odstc.logviewer.models.LogFile;
import com.netcracker.odstc.logviewer.models.Server;
import com.netcracker.odstc.logviewer.serverconnection.exceptions.ServerConnectionException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FTPServerConnection extends AbstractServerConnection {
    private final Logger logger = LogManager.getLogger(FTPServerConnection.class.getName());
    private final FTPClient ftpClient;

    public FTPServerConnection(Server server) {
        super(server);
        ftpClient = new FTPClient();
    }

    @Override
    public boolean connect() {
        logger.debug("Making connection to {}", server.getName());
        try {
            ftpClient.setConnectTimeout(CONNECT_TIMEOUT);
            ftpClient.connect(server.getIp(), server.getPort());
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                logger.error("Error with connect into {}", server.getIp());
                server.setConnectable(false);
            }
            server.setConnectable(ftpClient.login(server.getLogin(), server.getPassword()));
        } catch (IOException e) {
            server.setConnectable(false);
            logger.error("Error with connect into {}", server.getIp(), e);
        }
        isConnected = server.isConnectable();
        return server.isConnectable();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        try {
            ftpClient.disconnect();
        } catch (IOException e) {
            logger.error("Error with disconnect {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isDirectoryValid(Directory directory) {
        if (!super.isDirectoryValid(directory))
            return false;
        try {
            boolean isActive = ftpClient.changeWorkingDirectory(directory.getPath());
            if (isActive)
                ftpClient.changeToParentDirectory();
            return isActive;
        } catch (IOException e) {
            server.setConnectable(false);
            return false;
        }
    }

    protected List<Log> collectNewLogs() {
        List<Log> result = new ArrayList<>();
        try {
            for (int i = 0; i < directories.size(); i++) {
                HierarchyContainer directory = directories.get(i);
                result.addAll(extractLogsFromDirectory(directory));
                ftpClient.changeToParentDirectory();
            }
        } catch (IOException e) {
            throw new ServerConnectionException("Server Connection Problem", e);
        }
        return result;
    }

    protected void validateConnection() {
        if (!isConnected) {
            if (!connect()) {//I cant merge this one with enclosing one, because i dont need to connect() every time
                throw new ServerConnectionException("Cant establish connection with " + server.getIp());
            }
        }
    }

    private List<Log> extractLogsFromDirectory(HierarchyContainer directoryContainer) {
        if (directories.isEmpty()) {
            throw new ServerConnectionException("Server have empty list of active directories. Mark as cant be connected.");
        }
        List<Log> result = new ArrayList<>();
        Directory directory = (Directory) directoryContainer.getOriginal();
        directory.setLastExistenceCheck(new Date());
        try {
            if (!ftpClient.changeWorkingDirectory(directory.getPath())) {
                directory.setConnectable(false);
                return result;
            }
            for (int i = 0; i < directoryContainer.getChildren().size(); i++) {
                LogFile logFile = (LogFile) directoryContainer.getChildren().get(i).getOriginal();
                result.addAll(extractLogsFromFile(logFile));
            }
        } catch (IOException e) {
            logger.error("Marking directory as unavailable", e);
            directory.setConnectable(false);
        }
        directories.clear();
        return result;
    }

    private List<Log> extractLogsFromFile(LogFile logFile) {
        logFile.setLastUpdate(new Date());
        List<Log> result = new ArrayList<>();
        try {
            try (InputStream inputStream = ftpClient.retrieveFileStream(logFile.getName())) {
                result.addAll(extractLogsFromStream(inputStream, logFile));
                ftpClient.completePendingCommand();
            }
        } catch (IOException e) {
            logger.error("Error with reading file from", e);//TODO: Сделать Error - record
        }
        return result;
    }
}

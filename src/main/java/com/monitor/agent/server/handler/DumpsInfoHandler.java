package com.monitor.agent.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.agent.server.Server;
import com.monitor.agent.server.config.ConfigurationManager;
import com.monitor.agent.server.config.FilesConfig;
import com.monitor.agent.server.model.dumpsinfo.DumpInfoDto;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DumpsInfoHandler extends DefaultResponder {

    private static final Logger log = LoggerFactory.getLogger(DumpsInfoHandler.class);

    @Override
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {

        super.get(uriResource, urlParams, session);

        Server server = uriResource.initParameter(Server.class);
        ConfigurationManager configManager = server.getConfigManager();

        String response = "";
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        for (FilesConfig files : configManager.getConfig().getFiles()) {
            if (!"load_file_dates".equals(files.getSection())) {
                continue;
            }

            for (String path : files.getPaths()) {
                Path filePath = Paths.get(path);

                try (Stream<Path> stream = Files.list(filePath);) {
                    Map<String, String> fields = files.getFields();
                    String type = fields.getOrDefault("type", "time.csv");
                    Path timeFile = Paths.get(path + "/" + type);
                    if (!timeFile.toFile().exists()) {
                        Files.createFile(timeFile);
                        String string = objectMapper.writeValueAsString(formatter.format(new Date()));
                        Files.write(timeFile, string.getBytes());
                    }
                    String time;

                    Optional<String> first = Files.readAllLines(timeFile).stream().findFirst();
                    if (first.isPresent()) {
                        time = first.get();
                    } else {
                        time = formatter.format(new Date());
                        Files.write(timeFile, time.getBytes());
                    }

                    List<Path> directoryList = stream.collect(Collectors.toList());

                    Date date = formatter.parse(time);
                    List<DumpInfoDto> collect = directoryList
                            .stream()
                            .filter(file -> !Files.isDirectory(file))
                            .filter(file -> !file.getFileName().toString().equals(type))
                            .filter(file -> {
                                try {
                                    FileTime lastModifiedTime = Files.getLastModifiedTime(file);
                                    boolean after = isAfter(lastModifiedTime, date);
                                    if (after) {
                                        Files.write(timeFile, formatter.format(new Date()).getBytes());
                                    }
                                    return after;
                                } catch (IOException e) {
                                    log.error("Exception handling last modified time {}", file, e);
                                    return false;
                                }
                            })
                            .map(this::createDto)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    response = objectMapper.writeValueAsString(collect);
                } catch (Exception e) {
                    log.error("Exception while getting filePath", e);
                }
            }
        }

        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                NanoHTTPD.MIME_PLAINTEXT,
                response);
    }

    private boolean isAfter(FileTime fileTime, java.util.Date date) {
        return Date.from(fileTime.toInstant()).after(date);
    }

    private DumpInfoDto createDto(Path path) {
        try {
            DumpInfoDto dumpInfoDto = new DumpInfoDto();
            dumpInfoDto.setName(path.getFileName().toString());
            dumpInfoDto.setFullPath(path.toString());
            java.util.Date modified = Date.from(Files.getLastModifiedTime(path).toInstant());
            dumpInfoDto.setModifiedDate(modified.toString());
            dumpInfoDto.setSize(String.valueOf(Files.size(path)));
            return dumpInfoDto;
        } catch (IOException e) {
            log.error("Exception while getting filePath", e);
            return null;
        }

    }

    @Override
    public NanoHTTPD.Response post(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

}

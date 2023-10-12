# monitor-remote-agent

Агент собирает данные из логов технологического журнала 1С:Предприятие и PerfMon, отправляет их http-клиенту в формате JSON.
Используется в ПО Монитор https://1smonitor.ru, но может быть использоаан и как самостоятельное приложение для анализа записей логов технологического журнала 1С. Позволяет получать данные порциями и фильтровать записи.

Скомпилированный агент с возможностью установки в качестве службы Windows + исполняемый файл для Linux (собранный на GraalVM): https://private.1smonitor.ru/resources/monitor-remote-agent.zip

## Сборка Агента

Для сборки проекта используется Maven. Агент использует Java-библиотеки от 1С для работы с кластером, которые поставляются только в виде файлов на ИТС (https://its.1c.ru/db/metod8dev/content/4985/hdoc), поэтому нужно вручную установить эти библиотеки в локальный репозиторий Maven. Сделать это можно с помощью cmd-файла [/src/maven1c/maven1c.cmd](/src/maven1c/maven1c.cmd)

## Запуск Агента

В каталоге со скомпилированным файлом monitor-remote-agent.jar должен находиться конфигурационный файл, в котором указываются каталоги, из которых Агент будет читать лог-файлы. Примеры конфигурационных файлов можно найти в каталоге [/ext/config-samples](ext/config-samples).

В командной строке выполните команду:
```
java -jar monitor-remote-agent.jar -port 8085 -config config.json
```

Здесь `8085` - порт, на котором Агент будет принимать запросы от клиентов, а `config.json` - имя конфигурационного файла

При первом запуске Агента создастся рабочая папка `sincedb`, в которой будут храниться данные об уже прочитанных Агентом записях. Это сделано для возможности порционного чтения записей лог-файлов (см. параметр `ack` в примерах запросов к Агенту).

## Примеры запросов к Агенту

- http://localhost:8085

  Выведет краткий список поддерживаемых команд

- http://localhost:8085/logrecords

  Выдаст в формате JSON не более 1024 записей из всех файлов всех каталогов всех секций, указанных в конфигурационном файле

- http://localhost:8085/logrecords?max=100

  Выдаст в формате JSON не более 100 записей из всех файлов всех каталогов всех секций, указанных в конфигурационном файле

- http://localhost:8085/logrecords?section=common

  Выдаст в формате JSON записи из всех файлов всех каталогов секции "common", указанной в конфигурационном файле (`"section": "common"`)

- http://localhost:8085/logrecords?section=common&max=500&ack

  Выдаст в формате JSON не более 500 записей из всех файлов всех каталогов секции "common", указанной в конфигурационном файле (`"section": "common"`); `ack` - подтверждение получения данных клиентом - при следующем вызове Агента с этой же секцией Агент будет выдавать только новые, ранее непрочитанные данные; без `ack` будут возвращаться всегда записи с момента предыдущего вызова `ack` или с начала лог-файлов, если `ack` ни разу не применялся в запросе

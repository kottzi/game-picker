package io.github.kottzi.gamepicker.catalog.infrastructure.sync;

import io.github.kottzi.gamepicker.steam.SteamAppListClient;
import io.github.kottzi.gamepicker.steam.SteamStoreClient;
import io.github.kottzi.gamepicker.steam.dto.SteamAppListEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SteamCatalogSyncService {

    private static final Logger log = LoggerFactory.getLogger(SteamCatalogSyncService.class);

    private final SteamAppListClient appListClient;
    private final SteamStoreClient storeClient;
    private final GameCatalogWriter catalogWriter;
    private final int enrichBatchSize;

    public SteamCatalogSyncService(
            SteamAppListClient appListClient,
            SteamStoreClient storeClient,
            GameCatalogWriter catalogWriter,
            @Value("${steam.catalog.enrich-batch-size:1}") int enrichBatchSize
    ) {
        this.appListClient = appListClient;
        this.storeClient = storeClient;
        this.catalogWriter = catalogWriter;
        this.enrichBatchSize = enrichBatchSize;
    }

    @Scheduled(cron = "${steam.catalog.app-list-refresh-cron:0 0 4 * * *}")
    public void refreshAppList() {
        log.info("Обновление списка приложений Steam...");
        List<SteamAppListEntry> entries = appListClient.fetchAll();
        catalogWriter.upsertBareGames(entries);
        log.info("Список приложений Steam обновлён: {} записей", entries.size());
    }

    @Scheduled(fixedDelayString = "${steam.catalog.enrich-interval-ms:1500}")
    public void enrichNextBatch() {
        List<Long> pending = catalogWriter.findGameIdsMissingMetadata(enrichBatchSize);
        for (Long appId : pending) {
            try {
                storeClient.fetchAppDetails(appId)
                        .ifPresentOrElse(catalogWriter::applyMetadata,
                                () -> catalogWriter.markMetadataAttempted(appId));
            } catch (Exception e) {
                log.warn("Не удалось обогатить appid {}: {}", appId, e.getMessage());
                catalogWriter.markMetadataAttempted(appId);
            }
        }
    }
}

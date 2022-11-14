package org.sunbird.common.util;

import java.util.HashMap;
import java.util.Map;
import org.sunbird.cloud.storage.IStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import scala.Option;
import scala.Some;

public class CloudStorageUtil {
  private static final int STORAGE_SERVICE_API_RETRY_COUNT = 3;

  private static final Map<String, IStorageService> storageServiceMap = new HashMap<>();

  public static String upload(
      String storageType, String container, String objectKey, String filePath) {

    IStorageService storageService = getStorageService(storageType);

    return storageService.upload(
            container,
            filePath,
            objectKey,
            Option.apply(false),
            Option.apply(1),
            Option.apply(STORAGE_SERVICE_API_RETRY_COUNT),
            Option.empty());
  }

  public static String getSignedUrl(
      String storageType, String container, String objectKey) {
    IStorageService storageService = getStorageService(storageType);
    return getSignedUrl(storageService, container, objectKey);
  }

  public static String getSignedUrl(
      IStorageService storageService,
      String container,
      String objectKey) {
    int timeoutInSeconds = getTimeoutInSeconds();
    return storageService.getSignedURL(
        container, objectKey, Some.apply(timeoutInSeconds), Some.apply("r"));
  }

  private static IStorageService getStorageService(String storageType) {
    String storageKey = PropertiesCache.getInstance().getProperty(JsonKey.ACCOUNT_NAME);
    String storageSecret = PropertiesCache.getInstance().getProperty(JsonKey.ACCOUNT_KEY);
    return getStorageService(storageType, storageKey, storageSecret);
  }

  private static IStorageService getStorageService(
      String storageType, String storageKey, String storageSecret) {
    String compositeKey = storageType + "-" + storageKey;
    if (storageServiceMap.containsKey(compositeKey)) {
      return storageServiceMap.get(compositeKey);
    }
    synchronized (CloudStorageUtil.class) {
      StorageConfig storageConfig =
          new StorageConfig(storageType, storageKey, storageSecret);
      IStorageService storageService = StorageServiceFactory.getStorageService(storageConfig);
      storageServiceMap.put(compositeKey, storageService);
    }
    return storageServiceMap.get(compositeKey);
  }

  private static int getTimeoutInSeconds() {
    String timeoutInSecondsStr = ProjectUtil.getConfigValue(JsonKey.DOWNLOAD_LINK_EXPIRY_TIMEOUT);
    return Integer.parseInt(timeoutInSecondsStr);
  }

  public static String getUri(
      String storageType, String container, String prefix, boolean isDirectory) {
    IStorageService storageService = getStorageService(storageType);
    return storageService.getUri(container, prefix, Option.apply(isDirectory));
  }
}

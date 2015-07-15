package com.wikia.webdriver.common.templates;

import com.wikia.webdriver.common.contentpatterns.URLsContent;
import com.wikia.webdriver.common.core.CommonUtils;
import com.wikia.webdriver.common.core.annotations.NetworkTrafficDump;
import com.wikia.webdriver.common.core.configuration.Configuration;
import com.wikia.webdriver.common.core.geoedge.GeoEdgeProxy;
import com.wikia.webdriver.common.core.geoedge.GeoEdgeUtils;
import com.wikia.webdriver.common.core.networktrafficinterceptor.NetworkTrafficInterceptor;
import com.wikia.webdriver.common.core.urlbuilder.UrlBuilder;
import com.wikia.webdriver.common.driverprovider.NewDriverProvider;
import com.wikia.webdriver.common.logging.PageObjectLogging;

import org.browsermob.proxy.ProxyServer;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import java.io.File;
import java.lang.reflect.Method;

@Listeners({com.wikia.webdriver.common.logging.PageObjectLogging.class, com.wikia.webdriver.common.testnglisteners.InvokeMethodAdapter.class})
public class NewTestTemplateCore {

  protected WebDriver driver;
  protected UrlBuilder urlBuilder;
  protected String wikiURL;
  protected String wikiCorporateURL;
  protected String wikiCorpSetupURL;
  private DesiredCapabilities capabilities;
  protected NetworkTrafficInterceptor networkTrafficInterceptor;
  protected boolean isProxyServerRunning = false;

  @BeforeSuite(alwaysRun = true)
  public void beforeSuite() {
    prepareDirectories();
  }

  protected void prepareDirectories() {
    CommonUtils.deleteDirectory("." + File.separator + "logs");
    CommonUtils.createDirectory("." + File.separator + "logs");
  }

  protected void prepareURLs() {
    urlBuilder = new UrlBuilder();
    wikiURL = urlBuilder.getUrlForWiki(Configuration.getWikiName());
    wikiCorporateURL = urlBuilder.getUrlForWiki("wikia");
    wikiCorpSetupURL = urlBuilder.getUrlForWiki("corp");
  }

  protected void startBrowser() {
    driver = registerDriverListener(
        NewDriverProvider.getDriverInstanceForBrowser(Configuration.getBrowser())
    );
  }

  protected WebDriver startCustomBrowser(String browserName) {
    driver = registerDriverListener(
        NewDriverProvider.getDriverInstanceForBrowser(browserName)
    );
    return driver;
  }

  protected WebDriver registerDriverListener(EventFiringWebDriver driver) {
    driver.register(new PageObjectLogging());
    return driver;
  }

  protected void loadFirstPage() {
    driver.get(wikiURL + URLsContent.SPECIAL_VERSION);
  }

  protected void logOutCustomDriver(WebDriver customDriver) {
    customDriver.get(wikiURL + URLsContent.LOGOUT);
  }

  protected void stopBrowser() {
    /*if (NewDriverProvider.getMobileDriver() != null
        && NewDriverProvider.getMobileDriver().getSessionId() != null) {
      NewDriverProvider.getMobileDriver().quit();
    }*/
    if (driver != null) {
      driver.quit();
    }
  }

  protected void stopCustomBrowser(WebDriver customDriver) {
    if (customDriver != null) {
      customDriver.quit();
    }
  }

  protected DesiredCapabilities getCapsWithProxyServerSet(ProxyServer server) {
    capabilities = new DesiredCapabilities();
    try {
      capabilities.setCapability(
          CapabilityType.PROXY, server.seleniumProxy()
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return capabilities;
  }

  protected void setDriverCapabilities(DesiredCapabilities caps) {
    NewDriverProvider.setDriverCapabilities(caps);
  }

  protected void setWindowSize(int width, int height, WebDriver desiredDriver) {
    Dimension dimension = new Dimension(width, height);
    desiredDriver.manage().window().setSize(dimension);
  }

  protected void setBrowserUserAgent(String userAgent) {
    NewDriverProvider.setBrowserUserAgent(Configuration.getBrowser(), userAgent);
  }

  protected void runProxyServerIfNeeded(Method method) {
    boolean isGeoEdgeSet = false;
    boolean isNetworkTrafficDumpSet = false;
    String countryCode = null;

    if (method.isAnnotationPresent(GeoEdgeProxy.class)) {
      isGeoEdgeSet = true;
      countryCode = method.getAnnotation(GeoEdgeProxy.class).country();
    }

    if (method.isAnnotationPresent(NetworkTrafficDump.class)) {
      isNetworkTrafficDumpSet = true;
    }

    if (!isGeoEdgeSet && !isNetworkTrafficDumpSet) {
      return;
    }

    isProxyServerRunning = true;
    networkTrafficInterceptor = new NetworkTrafficInterceptor();
    networkTrafficInterceptor.startSeleniumProxyServer();
    if (isGeoEdgeSet && !countryCode.isEmpty()) {
      setGeoEdge(countryCode);
    }
    capabilities = getCapsWithProxyServerSet(networkTrafficInterceptor);
    setDriverCapabilities(capabilities);
  }

  public void setGeoEdge(String countryCode) {
    GeoEdgeUtils geoEdgeUtils = new GeoEdgeUtils();
    String credentialsBase64 = "Basic " + geoEdgeUtils.createBaseFromCredentials();
    String ip = geoEdgeUtils.getIPForCountry(countryCode);
    networkTrafficInterceptor.setProxyServer(ip);
    networkTrafficInterceptor.changeHeader("Proxy-Authorization", credentialsBase64);
  }
}

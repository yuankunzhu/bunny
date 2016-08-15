package org.rabix.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.rabix.common.helper.JSONHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunner {
  private static String testDirPath;
  private static String cmdPrefix;
  private static String resultPath = "./rabix-backend-local/target/result.yaml";
  private static String workingdir = "./rabix-backend-local/target/";
  private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

  public static void main(String[] commandLineArguments) {
    try {
      logger.info("Testing started...");	
      PropertiesConfiguration configuration = getConfig();
      testDirPath = getStringFromConfig(configuration, "testDirPath");
      cmdPrefix = getStringFromConfig(configuration, "cmdPrefix");
      startTestExecution();
    } catch (RabixTestException e) {
      logger.error("Error occuerred!", e);
      System.exit(-1);
    }
  }

  private static void startTestExecution() throws RabixTestException {
    boolean success = true;
    boolean testPassed = false;
    File dir = new File(testDirPath);
    File[] directoryListing = dir.listFiles();
    ArrayList<Object> failedTests = new ArrayList<Object>();

    if (!dir.isDirectory()) {
      logger.error("Problem with test directory path: Test directory path is not valid directory path.");
      System.exit(-1);
    }
    if (directoryListing == null) {
      logger.error("Problem with provided test directory: Test directory is empty.");
    }
    logger.info("Extracting jar file");
    executeCommand("sudo tar -zxvf " + System.getProperty("user.dir")
        + "/rabix-backend-local/target/rabix-backend-local-0.0.1-SNAPSHOT-id3.tar.gz");
    executeCommand("cp -a " + System.getProperty("user.dir") + "/rabix-integration-testing/testbacklog .");

    for (File child : directoryListing) {
      if (!child.toString().endsWith(".test.yaml"))
        continue;
      try {
        String currentTest = readFile(child.getAbsolutePath(), Charset.defaultCharset());
        Map<String, Object> inputSuite = JSONHelper.readMap(JSONHelper.transformToJSON(currentTest));
        Iterator entries = inputSuite.entrySet().iterator();
        while (entries.hasNext()) {
          Entry thisEntry = (Entry) entries.next();
          Object testName = thisEntry.getKey();
          Object test = thisEntry.getValue();

          logger.info("Running test: " + testName + " with given parameters:");
          @SuppressWarnings({ "rawtypes", "unchecked" })
          Map<String, Map<String, LinkedHashMap>> mapTest = (Map<String, Map<String, LinkedHashMap>>) test;
          logger.info("  app: " + mapTest.get("app"));
          logger.info("  inputs: " + mapTest.get("inputs"));
          logger.info("  expected: " + mapTest.get("expected"));
          String cmd = cmdPrefix + " " + mapTest.get("app") + " " + mapTest.get("inputs") + " > result.yaml";
          logger.info("->Running cmd: " + cmd);
          executeCommand(cmd);

          File resultFile = new File(resultPath);

          String resultText = readFile(resultFile.getAbsolutePath(), Charset.defaultCharset());
          Map<String, Object> resultData = JSONHelper.readMap(JSONHelper.transformToJSON(resultText));
          logger.info("\nGenerated result file:");
          logger.info(resultText);
          testPassed = validateTestCase(mapTest, resultData);
          logger.info("Test result: ");
          if (testPassed) {
            logger.info(testName + " PASSED");

          } else {
            logger.info(testName + " FAILED");
            failedTests.add(testName);
            success = false;
          }

        }

      } catch (IOException e) {
        logger.error("Test suite execution failed. ", e);
        System.exit(-1);
      }
    }

    if (success) {
      logger.info("Test suite passed successfully.");
    } else {
      logger.info("Test suite failed.");
      logger.info("Failed test number: " + failedTests.size());
      logger.info("Failed tests:");
      for (Object test : failedTests) {
        logger.info(test.toString());
      }

    }
  }

  private static boolean validateTestCase(Map<String, Map<String, LinkedHashMap>> mapTest,
      Map<String, Object> resultData) {
    String resultFileName;
    int resultFileSize;
    String resultFileClass;
    Map<String, Object> resultValues = ((Map<String, Object>) resultData.get("outfile"));
    resultFileName = resultValues.get("path").toString();
    resultFileName = resultFileName.split("/")[resultFileName.split("/").length - 1];
    resultFileSize = (int) resultValues.get("size");
    resultFileClass = resultValues.get("class").toString();
    logger.info("Test validation:");
    logger.info("result file name: " + resultFileName + ", expected file name: "
        + mapTest.get("expected").get("outfile").get("name"));
    logger.info("result file size: " + resultFileSize + ", expected file size: "
        + mapTest.get("expected").get("outfile").get("size"));
    logger.info("result file class: " + resultFileClass + ", expected file class: "
        + mapTest.get("expected").get("outfile").get("class"));

    boolean fileNamesEqual = resultFileName.equals(mapTest.get("expected").get("outfile").get("name"));
    boolean fileSizesEqual = resultFileSize == (int) mapTest.get("expected").get("outfile").get("size");
    boolean fileClassesEqual = resultFileClass.equals(mapTest.get("expected").get("outfile").get("class"));

    if (!fileNamesEqual) {
      logger.error("result and expected file name are not equal!");
    } else {
      if (!fileSizesEqual) {
        logger.error("result and expected file size are not equal!");
      } else {
        if (!fileClassesEqual) {
          logger.error("result and expected file class are not equal!");
        } else {
          logger.info("Test case passed.");	
          return true;
        }
      }
    }

    return false;
  }

  public static ArrayList<String> command(final String cmdline, final String directory) {
    try {
      Process process = new ProcessBuilder(new String[] { "bash", "-c", cmdline }).redirectErrorStream(true)
          .directory(new File(directory)).start();

      ArrayList<String> output = new ArrayList<String>();
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line = null;
      while ((line = br.readLine()) != null)
        output.add(line);

      if (0 != process.waitFor()) {
        return null;
      }

      return output;

    } catch (Exception e) {
      logger.error("Error while creating command. ", e);
      System.exit(-1);
    }
    return null;
  }

  
static void executeCommand(String cmdline) {
    ArrayList<String> output = command(cmdline, workingdir);
    if (null == output) {
      logger.error("COMMAND FAILED: " + cmdline + "\n");
//      System.exit(-1);
    }

    for (String line : output) {
      logger.info(line);
    }
  }

  /**
   * Reads content from a file
   */
  static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  @SuppressWarnings("unchecked")
  private static PropertiesConfiguration getConfig() throws RabixTestException {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    String userDir = System.getProperty("user.dir");
    if (userDir == null) {
      throw new RabixTestException("null value for user.dir property");
    }
    File configDir = new File(userDir + "/rabix-integration-testing/config/test");
    try {
      Iterator<File> iterator = FileUtils.iterateFiles(configDir, new String[] { "properties" }, true);
      while (iterator.hasNext()) {
        File configFile = iterator.next();
        configuration.load(configFile);
      }
      return configuration;
    } catch (ConfigurationException e) {
      logger.error("Failed to load configuration properties", e);
      throw new RabixTestException("Failed to load configuration properties");
    }
  }

  private static String getStringFromConfig(PropertiesConfiguration configuration, String key) {
    return configuration.getString(key);
  }

}

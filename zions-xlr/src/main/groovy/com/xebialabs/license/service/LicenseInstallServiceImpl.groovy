package com.xebialabs.license.service;

import com.xebialabs.license.AmountOfCisExceededException;
import com.xebialabs.license.License;
import com.xebialabs.license.LicenseEditionException;
import com.xebialabs.license.LicensePeriodExpiredException;
import com.xebialabs.license.LicenseProperty
import com.xebialabs.license.LicensePropertyMap
import com.xebialabs.license.LicenseReader;
import com.xebialabs.license.LicenseVersionException;
import com.xebialabs.license.LicenseViolationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List

import org.slf4j.Logger;

public class LicenseInstallServiceImpl implements LicenseInstallService
{
  private final File licenseFile;
  private final LicenseService licenseService;
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LicenseInstallServiceImpl.class);
  
  public LicenseInstallServiceImpl(String licensePath, LicenseService licenseService) {
    licenseFile = new File(licensePath);
    this.licenseService = licenseService;
  }
  


  boolean canInstallNewLicense()
  {
    if (!licenseFile.exists()) {
      return licenseFile.getParentFile().canWrite();
    }
    return licenseFile.canWrite();
  }
  
  public License installNewLicense(String licenseText) throws LicenseInstallationFailedException
  {
    if (!canInstallNewLicense()) {
      throw new LicenseInstallationFailedException("Could not install license because the license file could not be written to the file system.");
    }
    try {
      License tmpLicense = readLicense(licenseText);
      
      licenseService.validate(tmpLicense);
      
      String plainText = com.xebialabs.license.LicenseUtils.decodeIfNecessary(licenseText);
      
      java.nio.file.Files.write(licenseFile.toPath(), plainText.getBytes(StandardCharsets.UTF_8), new java.nio.file.OpenOption[0]);
      
      licenseService.reload();
      
      return tmpLicense;
    } catch (com.xebialabs.license.LicenseRepositoryIdException e) {
      throw new LicenseInstallationFailedException("Could not install license because the repository is locked to another license", e);
    } catch (LicenseViolationException e) {
      throw new LicenseInstallationFailedException("Could not install license because it is invalid", e);
    } catch (IOException e) {
      throw new LicenseInstallationFailedException("Could not install license due to problems with the file system", e);
    }
  }
  
  public String getLicenseRenewalMessage()
  {
    if (!licenseFile.exists()) {
      return null;
    }
    try
    {
      licenseService.validate();
    } catch (LicenseVersionException e) {
      logger.info("License status: ", e.getMessage());
      return "Your license is of an old version.";
    } catch (LicensePeriodExpiredException e) {
      logger.info("License status: ", e.getMessage());
      return "Your license has expired.";
    } catch (LicenseEditionException e) {
      logger.info("License status: ", e.getMessage());
      License license = licenseService.getLicense();
      if ((license != null) && ("Community".equals(license.getStringValue(com.xebialabs.license.LicenseProperty.EDITION)))) {
        return String.format("This edition of %s cannot be used with a Community Edition license.", [licenseService.getProduct()].toArray());
      }
      return String.format("This edition of %s requires a Trial Edition license or an Enterprise Edition license.", [licenseService.getProduct()].toArray());
    } catch (AmountOfCisExceededException e) {
      logger.info("License status: ", e.getMessage());
      //return "The number of configuration items in your repository exceeds the limit on this license.";
    } catch (LicenseViolationException e) {
      logger.info("License status: ", e.getMessage());
      return String.format("The current license is not valid (%s).", [e.getMessage() ].toArray());
    }
    return "Enter a new license key to renew your license.";
  }
  
  private License readLicense(String licenseText) throws IOException {
    File tempFile = File.createTempFile("license", "tmp");
    java.nio.file.Files.write(tempFile.toPath(), licenseText.getBytes(StandardCharsets.UTF_8), new java.nio.file.OpenOption[0]);
    //LicenseReader licenseReader = new LicenseReader();
    return InternalLicense.create();
  }
}


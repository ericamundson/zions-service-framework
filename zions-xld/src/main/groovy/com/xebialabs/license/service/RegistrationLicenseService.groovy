package com.xebialabs.license.service;

import com.xebialabs.deployit.repository.RepositoryMetadataService;
import com.xebialabs.license.DummyLicense;
import com.xebialabs.license.License;
import com.xebialabs.license.LicenseParseException;
import com.xebialabs.license.LicenseReader;
import com.xebialabs.license.LicenseViolationException;
import com.xebialabs.license.service.AbstractLicenseService;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;

public class RegistrationLicenseService extends AbstractLicenseService implements LicenseService
{
  private final LicenseReader licenseReader = new LicenseReader();
  
  public RegistrationLicenseService(String licensePath, String product, RepositoryMetadataService repositoryMetadataService) throws LicenseParseException, LicenseViolationException, IOException
  {
    super(licensePath, product, repositoryMetadataService);
    
    license = readLicense(licenseFile);
    
    if (license.isDateExpired()) {
      logLicenseError(licenseFile);
    }
  }
  
  public void validate() throws LicenseViolationException {
	  
  }
  
  public void validate(License license) throws LicenseViolationException
  {
	  
  }
  
  public void reload() throws LicenseViolationException, LicenseParseException
  {
	  
  }
  protected License readLicense(File licenseFile) throws LicenseParseException, LicenseViolationException {
    String licenseFilePath = licenseFile.getAbsolutePath();
    return InternalLicense.create();
    
//    log.info("Reading license from file {}", licenseFilePath);
//    License license = licenseReader.readLicense(licenseFile);
//    log.info(license.toLicenseContent());
//    
//    if (license.isDateExpired()) {
//      log.warn("*** License has expired.");
//    }
//    
//    return license;
  }
}

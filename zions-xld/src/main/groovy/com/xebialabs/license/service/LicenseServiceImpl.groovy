package com.xebialabs.license.service

import java.io.File
import java.util.ArrayList
import java.util.Collection
import java.util.List
import java.util.Map

import org.joda.time.LocalDate

import com.xebialabs.license.LicensePropertyMap
import com.xebialabs.license.License
import com.xebialabs.license.LicenseParseException
import com.xebialabs.license.LicenseProperty
import com.xebialabs.license.LicenseViolationException
import com.xebialabs.license.service.AbstractLicenseService
import com.xebialabs.license.service.LicenseService

class LicenseServiceImpl extends AbstractLicenseService {

	public LicenseServiceImpl(String licensePath, LicenseService licenseService) {
	}
	@Override
	protected License readLicense(File arg0) throws LicenseParseException, LicenseViolationException {
		return InternalLicense.create();
	}
	public void validate() throws LicenseViolationException {
	}
	public void validate(License license) throws LicenseViolationException {
	}

	public void reload() throws LicenseViolationException, LicenseParseException {
	}
}

class InternalLicense extends License {
	private LicensePropertyMap values;
	private InternalLicense(LicensePropertyMap values)
	throws LicenseViolationException {
		super(values);
		this.values = new LicensePropertyMap()
		this.values.set(LicenseProperty.PRODUCT,"XL Deploy")
		this.values.set(LicenseProperty.CONTACT,"Eric.Amundson2@zionsbancorp.com")
		this.values.set(LicenseProperty.LICENSE_VERSION,"3")
		this.values.set(LicenseProperty.EDITION,"Enterprise")
		this.values.set(LicenseProperty.LICENSED_TO,"Zions Bank")
		this.values.set(LicenseProperty.REPOSITORY_ID,"1.0.0.0")
		this.values.set(LicenseProperty.MAX_NUMBER_OF_USERS, "500")
		this.values.get(LicenseProperty.MAX_NUMBER_OF_CIS)
		this.values.set(LicenseProperty.MAX_NUMBER_OF_CIS, 'overthere.Host=500')
		this.values.set(LicenseProperty.MAX_NUMBER_OF_CIS, 'xl.Satellite=0')
		initPlugins()
		//this.values.set(LicenseProperty.LICENSED_PLUGINS, plugins)
		this.values.set(LicenseProperty.SUPPORT_POLICY, '8x5')

		Calendar i = Calendar.instance
		i.add(Calendar.YEAR,1)
		LocalDate out = LocalDate.fromCalendarFields(i)
		String sout = out.toString("yyyy-MM-dd")
		this.values.set(LicenseProperty.EXPIRES_AFTER, sout)
	}
	public static InternalLicense create() {
		try {
			return new InternalLicense(new LicensePropertyMap());
		} catch (LicenseViolationException e) {
			throw new RuntimeException("Unable to create bootstrap license", e);
		}
	}
	public String getStringValue(LicenseProperty key) {
		return values.getAsString(key);
	}
	public Map<String, Integer> getMapValue(LicenseProperty key) {
		return (Map)values.get(key);
	}

	public List<String> getListValue(LicenseProperty property) {
		return new ArrayList((Collection)values.get(property));
	}

	public boolean hasLicenseProperty(LicenseProperty key) {
		return values.containsKey(key);
	}

	@Override
	public List<LicenseProperty> getLicenseProperties() {
		return new ArrayList();
	}

	@Override
	public int getLicenseVersion() {
		return 3;
	}
	public LocalDate getLocalDateValue(LicenseProperty key) {
		Calendar i = Calendar.instance
		i.add(Calendar.YEAR,1)
		LocalDate out = LocalDate.fromCalendarFields(i)
		return out;
	}

	@Override
	public List<LicenseProperty> getRequiredProperties() {
		
		return new ArrayList();
	}
	public void validateLicenseFormat() {}
	public boolean isAtLeastVersion(int minimumLicenseVersion) {
		return true;
	}
	public boolean isDummyLicense() {
		return false;
	}

	public def initPlugins() {
		try {
			InputStream s = getClass().getResourceAsStream("/plugins.lst")
			this.values.get(LicenseProperty.LICENSED_PLUGINS)
			s.eachLine { line ->
				this.values.set(LicenseProperty.LICENSED_PLUGINS, line)
			}
		} catch (e) {}
	}
}

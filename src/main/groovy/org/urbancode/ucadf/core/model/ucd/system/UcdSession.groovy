/**
 * This class represents a UCD session.
 */
package org.urbancode.ucadf.core.model.ucd.system

import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.ws.rs.client.Client
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response

import org.glassfish.jersey.client.ClientConfig
import org.urbancode.ucadf.core.integration.jersey.JerseyManager
import org.urbancode.ucadf.core.model.ucadf.UcAdfObject
import org.urbancode.ucadf.core.model.ucadf.exception.UcAdfInvalidValueException

import com.fasterxml.jackson.annotation.JsonIgnore

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j

@Slf4j
@TypeChecked
class UcdSession extends UcAdfObject {
	public final static String PASSWORDISAUTHTOKEN = "PasswordIsAuthToken"
	
	// UrbanCode version matches.
    public final static String UCDVERSION_61 = /6\.1\..*/
    public final static String UCDVERSION_62 = /6\.2\..*/
	public final static String UCDVERSION_70 = /7\.0\..*/
	public final static String UCDVERSION_704 = /7\.0\.4\.*/
	public final static String UCDVERSION_71 = /7\.1\..*/
	
	// Session properties.	
	public final static String PROPUCDURL = "ucdUrl"
	public final static String PROPUCDUSERID = "ucdUserId"
	public final static String PROPUCDUSERPW = "ucdUserPw"
	public final static String PROPUCDAUTHTOKEN = "ucdAuthToken"
	public final static String PROPUCDAHWEBURL = "AH_WEB_URL"
	public final static String PROPUCDDSAUTHTOKEN = "DS_AUTH_TOKEN"

	/** The UCD instance URL. */
	private String ucdUrl
	
	/** The UCD instance user ID. */
	private String ucdUserId
	
	/** The UCD instance user password. */
	private String ucdUserPw
	
	/** The HTTP authentication string. */
	private String ucdHttpAuthStr
	
	/** The UCD instance version. */
	public String ucdVersion
	
	/** The UCD web client. */	
	private Client ucdWebClient
	
	/** The UCD non-compliant web client. */
	private Client ucdWebNonCompliantClient

	// Constructors.
	public UcdSession(
		final URL ucdUrl, 
		final String ucdUserId, 
		final String ucdUserPwOrToken) {
		
		this(
			ucdUrl.toString(), 
			ucdUserId, 
			ucdUserPwOrToken
		)
	}
	
	// Defaults to using URL and token values from the environment.
	UcdSession(
		final String ucdUrl, 
		final String ucdUserId, 
		final String ucdUserPwOrToken) {
		
		final Map<String, String> env = System.getenv()

		String derivedUcdUrl = ucdUrl
		if (!derivedUcdUrl) {
			derivedUcdUrl = env[PROPUCDAHWEBURL]
		}
		this.ucdUrl = derivedUcdUrl
		
		if (ucdUserId && ucdUserPwOrToken) {
			// If user ID and password provided then use that for BASIC authentication
			log.debug "Using provided ucdUserId and ucdUserPwOrToken"
			ucdHttpAuthStr = "$ucdUserId:$ucdUserPwOrToken"
			this.ucdUserId = ucdUserId
			this.ucdUserPw = ucdUserPwOrToken
		} else if (!ucdUserId && ucdUserPwOrToken) {
			// If no user ID provided and a password was provided then assume the password id a token
			log.debug "Using provided ucdUserPwOrToken"
			ucdHttpAuthStr = /PasswordIsAuthToken:{"token":"$ucdUserPwOrToken"}/
			this.ucdUserId = PASSWORDISAUTHTOKEN
			this.ucdUserPw = ucdUserPwOrToken
		} else {
			// If no user ID provided and no password provided then use the session token
			String ucdAuthToken = env[PROPUCDDSAUTHTOKEN]
			if (ucdAuthToken) {
				log.debug "Using session token from $PROPUCDDSAUTHTOKEN environment variable."
			}
		
			ucdHttpAuthStr = /PasswordIsAuthToken:{"token":"$ucdAuthToken"}/
			this.ucdUserId = PASSWORDISAUTHTOKEN
			this.ucdUserPw = ucdAuthToken
		}
	}
	
	public String getUcdUrl() {
		return ucdUrl
	}
	
	public void setUcdUrl(final String ucdUrl) {
		this.ucdUrl = ucdUrl
	}
	
	/**
	 * Get the UCD URL with the instance name replaced with an alias. Default to using the instance URL and optionally, replace the host name portion of the URL with the alias.
	 * @param alias The alias name to used to replace the instance name segment.
	 * @return URL The URL with the instance name segment replaced with the alias.
	 */
	public URL getUcdUrlForAlias(final String alias) {
		String derivedUrlStr = ucdUrl.toString()
		if (alias) {
			derivedUrlStr = derivedUrlStr.replaceAll(/^(http[s]?:\/\/)(.*?)(\..*)/, '$1' + alias + '$3')
		}
		
		return new URL(derivedUrlStr)
	}
	
	public String getUcdInstanceName() {
		return new URL(ucdUrl).getHost().split(/\./)[0]
	}

	public String getUcdInstanceId() {
		return getUcdInstanceName().replaceAll(/.*?(\d+)$/, '$1')
	}
	
	public String getUcdHttpAuthStr() {
		return ucdHttpAuthStr
	}
	
	public void setUcdHttpAuthStr(final String ucdHttpAuthStr) {
		this.ucdHttpAuthStr = ucdHttpAuthStr
	}

	public String getUcdUserId() {
		return ucdUserId
	}
	
	public setUcdUserId(final String ucdUserId) {
		this.ucdUserId = ucdUserId
	}

	public String getUcdUserPw() {
		return ucdUserPw
	}

	public void setUcdUserPw(final String ucdUserPw) {
		this.ucdUserPw = ucdUserPw
	}

	/**
	 * Get a UCD web target.
	 * @return The UCD web target.
	 */
	public WebTarget getUcdWebTarget() {
		if (!getUcdUrl()) {
			throw new UcAdfInvalidValueException("Unable to determine value for ucdUrl.")
		}

		if (!ucdWebClient) {
			ucdWebClient = getUcdConfiguredClient()
		}

		return ucdWebClient.target(getUcdUrl())
	}
	
	/**
	 * Get a non-compliant web client.
	 * @return The non-compliant web client.
	 */
	public Client getWebNonCompliantClient() {
		if (!ucdWebNonCompliantClient) {
			if (ucdUserId && ucdUserPw) {
				ucdWebNonCompliantClient = JerseyManager.getNonCompliantConfiguredClient(ucdUserId, ucdUserPw)
			} else {
				ucdWebNonCompliantClient = JerseyManager.getNonCompliantConfiguredClient(null, null)
			}
		}
		
		return ucdWebNonCompliantClient
	}
	
	// Get a configured client.
	private Client getUcdConfiguredClient(final ClientConfig config = null) {
		Client client
		if (ucdUserId && ucdUserPw) {
			client = JerseyManager.getConfiguredClient(ucdUserId, ucdUserPw, config)
		} else {
			client = JerseyManager.getConfiguredClient(null, null, config)
		}
		
		return client
	}
	
	/**
	 * Get the UCD version.
	 * @return The UCD version.
	 */
	@JsonIgnore
	public String getUcdVersion() {
        if (!ucdVersion) {
			Client client = JerseyManager.getConfiguredClient()

			WebTarget target = client.target(getUcdUrl())
			
			// By adding this header it doesn't require authentication.
			Response response = target.request().get()
			if (response.getStatus() != 200 && response.getStatus() != 401) {	
				log.error response.readEntity(String.class)
				throw new UcAdfInvalidValueException("Status: ${response.getStatus()} Unable to get UrbanCode Deploy version. $target")
			}
			
			String responseText = response.readEntity(String.class)

			// Attempt different pattern matches to get the version from the returned text.
			String returnUcdVersion
			String VERSION_PATTERN1 = /"(?s).*"version %s", "(.*?)".*/
			String VERSION_PATTERN2 = /.* class="productVersion">(.*)<.*/
			for (patternStr in [ VERSION_PATTERN1, VERSION_PATTERN2 ]) {
				Pattern pattern = Pattern.compile(patternStr)
				Matcher matcher = pattern.matcher(responseText)
				if (matcher.find()) {
					returnUcdVersion = matcher.group(1)
					break
				}
			}
			
            ucdVersion = returnUcdVersion.replaceAll("v. ", "")
			
            log.info "UCD Version [$ucdVersion]."
        }

        return ucdVersion
	}

	/**
	 * Determine if the UCD instance is using the specified version.
	 * @param versionRegex The version regular expression.
	 * @return True if the version matches.
	 */
	@JsonIgnore
	public Boolean isUcdVersion(final String versionRegex) {
        return (getUcdVersion() ==~ /$versionRegex/)
	}

	/**
	 * Compares the current session version to the specified version.
	 * @param versionb
	 * @return -1 if current session version is less than specified, 0 if equal, 1 if greater than.
	 */
	public Integer compareVersion(final String versionb) {
		compareVersion(getUcdVersion(), versionb)
	}
	
	/**
	 * Compare two version numbers.	
	 * @param versiona
	 * @param versionb
	 * @return
	 */
	public Integer compareVersion(final String versiona, final String versionb) {
		// This can handle the regex formatted number.
		List<String> versionaSegments = (versiona.split(/\./) as List<String>)*.replaceAll('\\\\', '')
		List<String> versionbSegments = (versionb.split(/\./) as List<String>)*.replaceAll('\\\\', '')
		
		Integer compareReturn = 0	
	
		// Determine which version has the least segments.
		Integer minSegments = Math.min(versionaSegments.size(), versionbSegments.size())
		
		for (Integer i = 0; i < minSegments; i++) {
			if (versionaSegments[i].isNumber() && versionbSegments[i].isNumber()) {
				Integer versionaNum = Integer.valueOf(versionaSegments[i])
				Integer versionbNum = Integer.valueOf(versionbSegments[i])
				compareReturn = versionaNum.compareTo(versionbNum)
			} else {
				compareReturn = versionaSegments[i].compareTo(versionbSegments[i])
			}
			
			if (compareReturn != 0) {
				break
			}
		}
	
		if (compareReturn == 0)	 {
			if (versionaSegments.size() < versionbSegments.size()) {
				compareReturn = -1
			} else if (versionaSegments.size() > versionbSegments.size()) {
				compareReturn = 1
			}
		}

		return compareReturn
	}
}

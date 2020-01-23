/**
 * This action returns a list of resource property objects.
 */
package org.urbancode.ucadf.core.action.ucd.resource

import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.Response

import org.urbancode.ucadf.core.actionsrunner.UcAdfAction
import org.urbancode.ucadf.core.model.ucd.exception.UcdInvalidValueException
import org.urbancode.ucadf.core.model.ucd.property.UcdProperty

class UcdGetResourceProperties extends UcAdfAction {
	/** The type of collection to return. */
	enum ReturnAsEnum {
		/** Return as a list. */
		LIST,
		
		/** Return as a map having the property name as the key. */
		MAPBYNAME
	}

	// Action properties.
	/** The resource path or ID. */
	String resource
	
	/** The type of collection to return. */
	ReturnAsEnum returnAs = ReturnAsEnum.LIST

	/** The flag that indicates fail if the resource is not found. Default is true. */
	Boolean failIfNotFound = true
	
	/**
	 * Runs the action.	
	 * @return The specified type of collection.
	 */
	@Override
	public Object run() {
		// Validate the action properties.
		validatePropsExist()

		List<UcdProperty> ucdProperties = []
		
		logVerbose("Getting resource [$resource] properties.")

		WebTarget target = ucdSession.getUcdWebTarget().path("/cli/resource/getProperties")
			.queryParam("resource", resource)
		logDebug("target=$target")

		Response response = target.request().get()
		if (response.getStatus() == 200) {
			ucdProperties = response.readEntity(new GenericType<List<UcdProperty>>(){})
		} else {
			if (response.getStatus() != 404 || failIfNotFound) {
				throw new UcdInvalidValueException(response)
			}
		}
		
		// Return as requested.
		Object resourceProperties
		Map<String, UcdProperty> propertiesMap = [:]
		if (ReturnAsEnum.LIST.equals(returnAs)) {
			resourceProperties = ucdProperties
		} else {
			ucdProperties.each {
				propertiesMap[it.getName()] = it.getValue()
			}
			resourceProperties = propertiesMap
		}
		
		return resourceProperties
	}
}

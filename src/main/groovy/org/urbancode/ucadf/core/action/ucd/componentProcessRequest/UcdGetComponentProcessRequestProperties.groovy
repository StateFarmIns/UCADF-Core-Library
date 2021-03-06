/**
 * This action gets a component process request's properties.
 */
package org.urbancode.ucadf.core.action.ucd.componentProcessRequest

import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.Response

import org.urbancode.ucadf.core.actionsrunner.UcAdfAction
import org.urbancode.ucadf.core.model.ucadf.exception.UcAdfInvalidValueException
import org.urbancode.ucadf.core.model.ucd.property.UcdProperty

class UcdGetComponentProcessRequestProperties extends UcAdfAction {
	/** The type of collection to return. */
	enum ReturnAsEnum {
		/** Return as a list. */
		LIST,
		
		/** Return as a map having the property name as the key. */
		MAPBYNAME
	}

	// Action properties.
	/** The component process request ID. */
	String requestId

	/** The type of collection to return. */
	ReturnAsEnum returnAs = ReturnAsEnum.MAPBYNAME

	/**
	 * Runs the action.	
	 * @return The specified type of collection.
	 */
	@Override
	public Object run() {
		// Validate the action properties.
		validatePropsExist()

		Object processProperties
		
		WebTarget target = ucdSession.getUcdWebTarget().path("/rest/deploy/componentProcessRequest/{requestId}/properties")
			.resolveTemplate("requestId", requestId)
		logDebug("target=$target")
		
		Response response = target.request().get()
		if (response.getStatus() == 200) {
			Map responseMap = response.readEntity(new GenericType<Map<String, List<UcdProperty>>>(){})
			List<UcdProperty> ucdProperties = responseMap.get("properties")
			if (ReturnAsEnum.LIST.equals(returnAs)) {
				processProperties = ucdProperties
			} else {
				processProperties = [:]
				for (ucdProperty in ucdProperties) {
					processProperties.put(ucdProperty.getName(), ucdProperty)
				}
			}
		} else {
            throw new UcAdfInvalidValueException(response)
		}
		
		return processProperties
	}	
}

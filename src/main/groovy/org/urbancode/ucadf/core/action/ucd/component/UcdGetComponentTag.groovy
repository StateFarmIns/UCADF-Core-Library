/**
 * This action gets a component tag.
 */
package org.urbancode.ucadf.core.action.ucd.component

import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response

import org.urbancode.ucadf.core.actionsrunner.UcAdfAction
import org.urbancode.ucadf.core.model.ucd.exception.UcdInvalidValueException
import org.urbancode.ucadf.core.model.ucd.tag.UcdTag

class UcdGetComponentTag extends UcAdfAction {
	// Action properties.
	/** The component tag name or ID. */
	String tag
	
	/** The flag that indicates fail if the component tag is not found. Default is true. */
	Boolean failIfNotFound = true
	
	/**
	 * Runs the action.	
	 * @return The tag object.
	 */
	@Override
	public UcdTag run() {
		// Validate the action properties.
		validatePropsExist()

		logInfo("Getting component tag [$tag].")
	
		UcdTag ucdTag
		
		WebTarget target = ucdSession.getUcdWebTarget().path("/rest/tag/type/Component/name/{tag}")
			.resolveTemplate("tag", tag)
		logDebug("target=$target")
		
		Response response = target.request().get()
		if (response.getStatus() == 200) {
			ucdTag = response.readEntity(UcdTag.class)
		} else {
			String errMsg = UcdInvalidValueException.getResponseErrorMessage(response)
			logInfo(errMsg)
			if (response.getStatus() != 400 || failIfNotFound) {
				throw new UcdInvalidValueException(errMsg)
			}
		}
		
		return ucdTag
	}
}

/**
 * This action deletes a snapshot.
 */
package org.urbancode.ucadf.core.action.ucd.snapshot

import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import org.urbancode.ucadf.core.actionsrunner.UcAdfAction
import org.urbancode.ucadf.core.model.ucd.exception.UcdInvalidValueException
import org.urbancode.ucadf.core.model.ucd.snapshot.UcdSnapshot

import groovy.util.logging.Slf4j;

class UcdDeleteSnapshot extends UcAdfAction {
	// Action properties.
	/** The application name or ID. */
	String application
	
	/** The snapshot name or ID. */
	String snapshot
	
	/** The flag that indicates fail if the snapshot is not found. Default is false. */
	Boolean failIfNotFound = false

	/**
	 * Runs the action.	
	 * @return True if the snapshot was deleted.
	 */
	@Override
	public Boolean run() {
		// Validate the action properties.
		validatePropsExist()

		Boolean deleted = false
		
		logInfo("Deleting application [$application] snapshot [$snapshot].")

		UcdSnapshot ucdSnapshot = actionsRunner.runAction([
			action: UcdGetSnapshot.getSimpleName(),
			application: application,
			snapshot: snapshot,
			failIfNotFound: failIfNotFound
		])

		if (ucdSnapshot) {
			WebTarget target = ucdSession.getUcdWebTarget().path("/cli/snapshot/deleteSnapshot")
				.queryParam("snapshot", snapshot)
				.queryParam("application", application)
			logDebug("target=$target")
			
			Response response = target.request(MediaType.APPLICATION_JSON).delete()
			if (response.getStatus() == 204) {
				logInfo("Application [$application] snapshot [$snapshot] deleted.")
				deleted = true
			} else {
				String errMsg = UcdInvalidValueException.getResponseErrorMessage(response)
				logInfo(errMsg)
				if (response.getStatus() != 404 || failIfNotFound) {
					throw new UcdInvalidValueException(errMsg)
				}
			}
		}
		
		return deleted
	}
}

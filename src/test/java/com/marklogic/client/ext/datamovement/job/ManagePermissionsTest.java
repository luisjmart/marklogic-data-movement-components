package com.marklogic.client.ext.datamovement.job;

import com.marklogic.client.datamovement.DeleteListener;
import com.marklogic.client.ext.batch.RestBatchWriter;
import com.marklogic.client.ext.batch.SimpleDocumentWriteOperation;
import com.marklogic.client.ext.datamovement.AbstractDataMovementTest;
import com.marklogic.client.ext.datamovement.QueryBatcherTemplate;
import com.marklogic.client.ext.helper.ClientHelper;
import com.marklogic.client.io.DocumentMetadataHandle;
import org.junit.Test;

import java.util.Arrays;

public class ManagePermissionsTest extends AbstractDataMovementTest {

	/**
	 * Note that with ML9 and previous versions, rest-reader/read and rest-writer/update are always added to documents
	 * inserted via /v1/documents.
	 * On ML10 the rest permissions are no longer added automatically
	 */
	@Test
	public void test() {
		final String uri = "/test/manage-permissions-test.xml";

		QueryBatcherTemplate qbt = new QueryBatcherTemplate(newClient("Documents"));

		// Clear out the test documents
		qbt.applyOnDocumentUris(new DeleteListener(), uri);

		// Insert documents
		RestBatchWriter writer = new RestBatchWriter(client, false);
		writer.write(Arrays.asList(
			new SimpleDocumentWriteOperation(uri, "<one/>").
				addPermissions("app-user", DocumentMetadataHandle.Capability.READ, DocumentMetadataHandle.Capability.UPDATE)
		));
		writer.waitForCompletion();

		ClientHelper helper = new ClientHelper(client);
		DocumentMetadataHandle.DocumentPermissions perms = helper.getMetadata(uri).getPermissions();
		assertEquals(1, perms.size());
		assertEquals(2, perms.get("app-user").size());
		assertTrue(perms.get("app-user").contains(DocumentMetadataHandle.Capability.READ));
		assertTrue(perms.get("app-user").contains(DocumentMetadataHandle.Capability.UPDATE));

		// Set permissions
		new SetPermissionsJob("alert-user", "read", "alert-user", "update").setWhereUris(uri).run(client);
		perms = helper.getMetadata(uri).getPermissions();
		assertEquals(1, perms.size());
		assertEquals(2, perms.get("alert-user").size());
		assertTrue(perms.get("alert-user").contains(DocumentMetadataHandle.Capability.READ));
		assertTrue(perms.get("alert-user").contains(DocumentMetadataHandle.Capability.UPDATE));

		// Add permissions
		new AddPermissionsJob("app-user", "read", "app-user", "update").setWhereUris(uri).run(client);
		perms = helper.getMetadata(uri).getPermissions();
		assertEquals(2, perms.size());
		assertEquals(2, perms.get("alert-user").size());
		assertTrue(perms.get("alert-user").contains(DocumentMetadataHandle.Capability.READ));
		assertTrue(perms.get("alert-user").contains(DocumentMetadataHandle.Capability.UPDATE));
		assertEquals(2, perms.get("app-user").size());
		assertTrue(perms.get("app-user").contains(DocumentMetadataHandle.Capability.READ));
		assertTrue(perms.get("app-user").contains(DocumentMetadataHandle.Capability.UPDATE));

		// Remove permissions
		new RemovePermissionsJob("app-user", "read", "app-user", "update").setWhereUris(uri).run(client);
		perms = helper.getMetadata(uri).getPermissions();
		assertEquals(1, perms.size());
		assertEquals(2, perms.get("alert-user").size());
		assertTrue(perms.get("alert-user").contains(DocumentMetadataHandle.Capability.READ));
		assertTrue(perms.get("alert-user").contains(DocumentMetadataHandle.Capability.UPDATE));
	}
}

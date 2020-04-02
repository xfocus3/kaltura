import com.kaltura.client.enums.*;
import com.kaltura.client.types.*;
import com.kaltura.client.services.*;
import com.kaltura.client.KalturaApiException;
import com.kaltura.client.KalturaClient;
import com.kaltura.client.KalturaConfiguration;
import chunkedupload.ParallelUpload;
import java.io.File;


public class UploadTest { 
	public static void main(String[] argv){
		
			try{
				KalturaConfiguration config = new KalturaConfiguration();
				config.setEndpoint("https://www.kaltura.com");
				KalturaClient client = new KalturaClient(config);

				String secret = "b2a56dea9865f22c7007da573011f306";
				String userId = null;
				int partnerId = Integer.parseInt("2725931");
				String privileges = null;
				KalturaSessionService sessionService = client.getSessionService();
				String ks = client.generateSessionV2(secret, null, KalturaSessionType.ADMIN, partnerId, 86400, "");

				client.setSessionId(ks);
				System.out.println(ks);

				KalturaMediaEntry newEntry = null;
				boolean update = false;

				final File file = new File(argv[0]);
			for(final File child : file.listFiles()) {
				if(child.isDirectory())
					continue;
			try{
					KalturaMediaEntry entry = new KalturaMediaEntry();
					entry.name = child.getName().replaceFirst("[.][^.]+$", "");
					entry.type = KalturaEntryType.MEDIA_CLIP;
					entry.mediaType = KalturaMediaType.VIDEO;
					newEntry = client.getMediaService().add(entry);

				System.out.println("\nCreated a new entry: " + newEntry.id);
				
				ParallelUpload pu = new ParallelUpload(client, child.getCanonicalPath());	
				String tokenId = pu.upload();
				if (tokenId != null) {
					KalturaUploadedFileTokenResource fileTokenResource = new KalturaUploadedFileTokenResource();
					fileTokenResource.token = tokenId;
					if(update == true) {
						newEntry = client.getMediaService().updateContent(newEntry.id, fileTokenResource);
					} else {
						newEntry = client.getMediaService().addContent(newEntry.id, fileTokenResource);
					}
					System.out.println("\nUploaded a new Video file to entry: " + newEntry.id);
				}
			} catch (KalturaApiException e) {
			            e.printStackTrace();
			}

			}

}catch (Exception exc) {
        	exc.printStackTrace();
    	}
    }
}

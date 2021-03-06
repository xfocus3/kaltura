import com.kaltura.client.APIOkRequestsExecutor;
import com.kaltura.client.Client;
import com.kaltura.client.Configuration;
import com.kaltura.client.ILogger;
import com.kaltura.client.Logger;
import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.enums.EntryType;
import com.kaltura.client.enums.MediaType;
import com.kaltura.client.enums.SessionType;
import com.kaltura.client.services.MediaService;
import com.kaltura.client.services.MediaService.AddContentMediaBuilder;
import com.kaltura.client.services.MediaService.AddMediaBuilder;
import com.kaltura.client.services.MediaService.GetMediaBuilder;
import com.kaltura.client.services.SessionService;
import com.kaltura.client.services.UploadTokenService;
import com.kaltura.client.services.UploadTokenService.AddUploadTokenBuilder;
import com.kaltura.client.services.UploadTokenService.UploadUploadTokenBuilder;
import com.kaltura.client.types.APIException;
import com.kaltura.client.types.MediaEntry;
import com.kaltura.client.types.Partner;
import com.kaltura.client.services.PartnerService;
import com.kaltura.client.services.PartnerService.GetPartnerBuilder;
import com.kaltura.client.types.UploadToken;
import com.kaltura.client.types.UploadedFileTokenResource;
import com.kaltura.client.utils.request.NullRequestBuilder;
import com.kaltura.client.utils.request.RequestElement;
import com.kaltura.client.utils.response.OnCompletion;
import com.kaltura.client.utils.response.base.Response;

import chunkedupload.ParallelUpload;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public class UploadTest { 
	public static void main(String[] argv){
		
			try{
				Configuration config = new Configuration();
				config.setEndpoint("https://www.kaltura.com");
				Client client = new Client(config);

				String secret = "b2a56dea9865f22c7007da573011f306";
				String userId = null;
				int partnerId = Integer.parseInt("2725931");
				String privileges = null;
				String ks = client.generateSessionV2(secret, null, SessionType.ADMIN, partnerId, 86400, "");

				client.setSessionId(ks);
				System.out.println(ks);

				MediaEntry newEntry = null;
				boolean update = false;

				final File file = new File(argv[0]);
			for(final File child : file.listFiles()) {
				System.out.println("File Name"+child.getName());
				if(child.isDirectory())
					continue;
				MediaEntry entry = new MediaEntry();
				entry.setName( child.getName().replaceFirst("[.][^.]+$", ""));
				System.out.println("entry.name  "+entry.getName());

				entry.setType(EntryType.MEDIA_CLIP);
				entry.setMediaType(MediaType.VIDEO);
				System.out.println("\nCreating a new entry: " + entry.getId());
				final File fileData = child;

				final CountDownLatch doneSignal = new CountDownLatch(1);
				MediaService.UploadMediaBuilder requestBuilder = MediaService.upload(fileData)
						.setCompletion(new OnCompletion<Response<String>>() {

							@Override
							public void onComplete(Response<String> result) {

								MediaEntry entry = new MediaEntry();
								entry.setName(child.getName());
								entry.setType(EntryType.MEDIA_CLIP);
								entry.setMediaType(MediaType.VIDEO);

								MediaService.AddFromUploadedFileMediaBuilder requestBuilder = MediaService.addFromUploadedFile(entry, result.results)
										.setCompletion(new OnCompletion<Response<MediaEntry>>() {

											@Override
											public void onComplete(Response<MediaEntry> result) {
												MediaEntry entry = result.results;

												doneSignal.countDown();
											}
										});
								APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));
							}
						});
				APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));
				doneSignal.await();

				//				addTestImage(client,child,null);
				/*
				AddMediaBuilder requestBuilder = MediaService.add(entry)
						.setCompletion(new OnCompletion<Response<MediaEntry>>() {
							@Override
							public void onComplete(Response<MediaEntry> result) {
								final MediaEntry[] newEntry = {result.results};


								System.out.println("\nCreated a new entry: " + newEntry[0].getId());
								ParallelUpload pu = null;
								try {

									pu = ParallelUpload.getInstance(newEntry[0],client, child.getCanonicalPath());
								} catch (IOException e) {
									e.printStackTrace();
								}
								String tokenId = null;
								try {
									tokenId = pu.upload();
								} catch (InterruptedException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								} catch (APIException e) {
									e.printStackTrace();
								}
								if (tokenId != null) {
									UploadedFileTokenResource fileTokenResource = new UploadedFileTokenResource();
									fileTokenResource.setToken( tokenId);
									if(update == true) {
										// Define content
										UploadedFileTokenResource resource = new UploadedFileTokenResource();
										resource.setToken(tokenId);
										MediaService.UpdateContentMediaBuilder requestBuilder = MediaService.updateContent(entry.getId(), fileTokenResource)
												.setCompletion(new OnCompletion<Response<MediaEntry>>() {

													@Override
													public void onComplete(Response<MediaEntry> result) {

														newEntry[0] = result.results;}});
										APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));

									} else {
										AddContentMediaBuilder requestBuilder = MediaService.addContent(entry.getId(), fileTokenResource)
												.setCompletion(new OnCompletion<Response<MediaEntry>>() {

													@Override
													public void onComplete(Response<MediaEntry> result) {
														newEntry[0] = result.results;}});
										APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));

									}
									System.out.println("\nUploaded a new Video file to entry: " + newEntry[0].getId());
								}
							}});
				APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));
				System.out.println("finish " );
*/

			}

}catch (Exception exc) {
        	exc.printStackTrace();
    	}
    }


	public static void addTestImage(Client client, File file, final OnCompletion<MediaEntry> onCompletion)
	{
		MediaEntry entry = new MediaEntry();
		entry.setName(file.getName());
		entry.setMediaType(MediaType.IMAGE);

		final File fileData;
		final long fileSize;
		fileData = file;
		fileSize = fileData.length();

		AddMediaBuilder requestBuilder = MediaService.add(entry)
				.setCompletion(new OnCompletion<Response<MediaEntry>>() {

					@Override
					public void onComplete(Response<MediaEntry> result) {
						final MediaEntry entry = result.results;

						// Upload token
						UploadToken uploadToken = new UploadToken();
						uploadToken.setFileName(file.getName());
						uploadToken.setFileSize((double) fileSize);
						AddUploadTokenBuilder requestBuilder = UploadTokenService.add(uploadToken)
								.setCompletion(new OnCompletion<Response<UploadToken>>() {

									@Override
									public void onComplete(Response<UploadToken> result) {
										final UploadToken token = result.results;

										// Define content
										UploadedFileTokenResource resource = new UploadedFileTokenResource();
										resource.setToken(token.getId());
										AddContentMediaBuilder requestBuilder = MediaService.addContent(entry.getId(), resource)
												.setCompletion(new OnCompletion<Response<MediaEntry>>() {

													@Override
													public void onComplete(Response<MediaEntry> result) {

														// upload
														UploadUploadTokenBuilder requestBuilder = UploadTokenService.upload(token.getId(), fileData, false)
																.setCompletion(new OnCompletion<Response<UploadToken>>() {

																	@Override
																	public void onComplete(Response<UploadToken> result) {
																		MediaService.get(entry.getId());
																		onCompletion.onComplete(entry);
																	}
																});
														APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));
													}
												});
										APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));
									}
								});
						APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));
					}
				});
		APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));
	}

}

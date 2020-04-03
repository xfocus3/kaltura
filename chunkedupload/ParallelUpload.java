// ===================================================================================================
//                           _  __     _ _
//                          | |/ /__ _| | |_ _  _ _ _ __ _
//                          | ' </ _` | |  _| || | '_/ _` |
//                          |_|\_\__,_|_|\__|\_,_|_| \__,_|
//
// This file is part of the Kaltura Collaborative Media Suite which allows users
// to do with audio, video, and animation what Wiki platfroms allow them to do with
// text.
//
// Copyright (C) 2006-2017  Kaltura Inc.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// @ignore
// ===================================================================================================
package chunkedupload;

import com.kaltura.client.APIOkRequestsExecutor;
import com.kaltura.client.Client;
import com.kaltura.client.ILogger;
import com.kaltura.client.Logger;
import com.kaltura.client.services.UploadTokenService;
import com.kaltura.client.services.UploadTokenService.AddUploadTokenBuilder;
import com.kaltura.client.services.UploadTokenService.UploadUploadTokenBuilder;
import com.kaltura.client.types.APIException;
import com.kaltura.client.types.MediaEntry;
import com.kaltura.client.types.UploadToken;
import com.kaltura.client.utils.response.OnCompletion;
import com.kaltura.client.utils.response.base.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ParallelUpload {
	public static  ParallelUpload INSTANCE;
	public  static  ParallelUpload getInstance(MediaEntry mediaEntry, Client client, String canonicalPath){
		if (INSTANCE == null) {
			INSTANCE = new ParallelUpload(mediaEntry, client, canonicalPath);
		}
		return INSTANCE;
	}
	public static ParallelUpload getINSTANCE(){
		return INSTANCE;
	}
	/**
	 * The ChunkedStream class wraps a FileInputStream and limit the size avaialble for reading
	 * It's required for enabling the http multipart to send partial files using the FilePartSource interface
	 */
	private class ChunkedStream extends FileInputStream
	{
		int size;
		int bytesLeft;

		public int getSize()
		{
			return size;
		}

		public ChunkedStream(String name) throws FileNotFoundException
		{
			super(name);
		}

		public void resetChunk(long seek, int _size) throws IOException
		{
			bytesLeft = size = _size;
			this.getChannel().position(seek);
		}

		public int read(byte[] b) throws IOException
		{
			if (bytesLeft == 0)
				return -1;

			int readSize = b.length < size ? b.length : size;
			bytesLeft -= readSize;
			return super.read(b, 0, readSize);
		}
	
		/*
		 * prevent the http request from closing the file so it will be reused throughout the thread
		 */	
		public void close()
		{
		}

		public void forceClose() throws IOException
		{
			super.close();
		}
	}

	/**
	 * The UploadTask class implements a thread which uploads chunks.
	 * The code loops and retreives the next chunk to be uploaded from the parent class until there are no more chunks to upload
	 */
	private class UploadTask implements Runnable {
		public ParallelUpload pu;

		public void run()
		{
			try {
				String threadName = Thread.currentThread().getName();
				ChunkedStream stream = new ChunkedStream(pu.fileName);

				while(true)
				{

					// get next chunk for upload
					int i = pu.getNextChunk();
					if (i == -1)
					{
						stream.forceClose();
						return;
					}

					// calculate seek and size for chunk
					long seekPos = (long)i * pu.chunkSize;
					int size = (int)Math.min(pu.chunkSize, pu.fileSize - seekPos);

					int chunkRetries = 0;
					boolean success = false;
					do
					{
			        		log.info(String.format("%s: chunk %d pos %d size %d", threadName, i, seekPos, size));

						stream.resetChunk(seekPos, size);
						success = pu.addChunk(stream, true, (seekPos + size) == pu.fileSize, seekPos);
						if (success)
						{
							pu.addUploadSize(size);
							break;
						}

						chunkRetries++;
					} while(pu.countRetries(chunkRetries));
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				pu.countRetries(pu.maxChunkRetries);
			}
		}
	}

	private ILogger log = Logger.getLogger(getClass());

	private String fileName;
	private long fileSize;
	private int nextChunk = 0;
	private int chunkCount = 0;
	private long uploadSize = 0;
	private Client client;
	private  MediaEntry entry;
	private UploadToken upToken;
	private int retryCount = 0;

	public int chunkSize = 10*1024*1024;
	public int threadCount = 10;
	public int maxChunkRetries = 3;
	public int maxRetries = 5;

	private synchronized boolean countRetries(int chunkRetries)
	{
		if (chunkRetries < maxChunkRetries)
			retryCount++;
		else
			retryCount = maxRetries + 1;

		return retryCount > maxRetries;
	}

	private synchronized int getNextChunk()
	{
		if (retryCount < maxRetries && nextChunk < chunkCount)
			return nextChunk++;

		return -1; 
	}

	private synchronized void addUploadSize(long size)
	{
		uploadSize += size;
	}

	public ParallelUpload(MediaEntry newEntry, Client _client, String _fileName)
	{
		client = _client;
		fileName = _fileName;
		entry =newEntry;
	}

	public String upload() throws InterruptedException, IOException, APIException
	{
		File fileData = new File(fileName);
		fileSize = fileData.length();

		ChunkedStream stream;
		stream = new ChunkedStream(fileName);

		stream.resetChunk(0, 1);
		// Upload token
		UploadToken uploadToken = new UploadToken();
		uploadToken.setFileName(fileName);
		uploadToken.setFileSize((double) fileSize);
		AddUploadTokenBuilder requestBuilder = UploadTokenService.add(uploadToken)
				.setCompletion(new OnCompletion<Response<UploadToken>>() {

					@Override
					public void onComplete(Response<UploadToken> result) {

						upToken = result.results;

						chunkCount = (int) ((fileSize + chunkSize - 1) / chunkSize);
						log.info("Uploading token " + upToken.getId() + " file size " + fileSize + " in " + chunkCount + " chunks");

						// add the first byte and then parallelize the actual upload
						try {
							addChunk(stream, false, false, 0);
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							stream.forceClose();
						} catch (IOException e) {
							e.printStackTrace();
						}
						log.info("Uploading token " + upToken.getId() + " file size " + fileSize + " uploaded " + uploadSize);
						List<Thread> threads = new ArrayList<Thread>();

						for (int i = 0; i < threadCount; i++) {
							UploadTask uploadTask = new UploadTask();
							uploadTask.pu = getINSTANCE();

							Thread t = new Thread(uploadTask);
							threads.add(t);
							t.start();
						}

						for (Thread t : threads) {
							try {
								t.join();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}


					}});
		APIOkRequestsExecutor.getExecutor().queue(requestBuilder.build(client));


		return uploadSize == fileSize ? upToken.getId() : null;
 	}

    /**
     * @param ChunkedStream stream
     * @param boolean resume
     * @param boolean finalChunk
     * @param long resumeAt
     *
     * @return
     */
    private boolean addChunk(ChunkedStream stream, boolean resume, boolean finalChunk, long resumeAt) throws IOException {
		// upload
		UploadUploadTokenBuilder requestBuilder = UploadTokenService.upload(upToken.getId(), stream, "a.mkv", String.valueOf(stream.getSize()), resume, finalChunk, resumeAt);
		return true;
    }
}

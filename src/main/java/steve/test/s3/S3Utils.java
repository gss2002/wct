//package steve.test.s3;
//
//import org.jets3t.service.S3Service;
//import org.jets3t.service.impl.rest.httpclient.RestS3Service;
//import org.jets3t.service.model.S3Bucket;
//
//public class S3Utils {
//
//
//	private static String bucketName = "noaa-nexrad-level2"; 
//	private static String key        = "2015/05/15/KVWX/KVWX20150515_080737_V06.gz";      
//
//
//
//	public static void main(String[] args) {
//
//		try {
//
//			S3Service s3Service = new RestS3Service(null);
//			S3Bucket bucket = s3Service.getBucket(bucketName);
//
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//
//}
//
//

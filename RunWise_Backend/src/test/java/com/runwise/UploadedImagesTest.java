package com.runwise;

import com.runwise.common.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class UploadedImagesTest {
    @TempDir Path root;
    @Test void rejectsDisguisedFilesAndDecodesRealImages() throws Exception {
        assertThrows(BusinessException.class,()->UploadedImages.sanitize("<svg><script/></svg>".getBytes()));
        for(String format:new String[]{"png","jpeg"}) {
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(5,5,BufferedImage.TYPE_INT_RGB),format,output);
            UploadedImages.Image image=UploadedImages.sanitize(output.toByteArray());
            assertNotNull(ImageIO.read(new ByteArrayInputStream(image.bytes)));
            assertEquals(format.equals("png")?".png":".jpg",image.extension);
        }
    }
    @Test void onlyExistingOwnPhotosCanBeAttachedToNewRecords() throws Exception {
        String filename="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png";
        Path directory=Files.createDirectories(root.resolve("checkin/7"));Files.write(directory.resolve(filename),new byte[]{1});
        String url="/uploads/checkin/7/"+filename;
        assertDoesNotThrow(()->UploadedImages.requireOwned(url,null,7L,root.toString()));
        assertThrows(BusinessException.class,()->UploadedImages.requireOwned(url,null,8L,root.toString()));
        assertThrows(BusinessException.class,()->UploadedImages.requireOwned("https://example.com/photo.png",null,7L,root.toString()));
        assertThrows(BusinessException.class,()->UploadedImages.requireOwned("/uploads/checkin/7/../8/"+filename,null,7L,root.toString()));
        assertDoesNotThrow(()->UploadedImages.requireOwned("/uploads/checkin/old.png","/uploads/checkin/old.png",7L,root.toString()));
    }
    @Test void webpRemainsSupportedAfterRealDecodingWasIntroduced() throws Exception {
        byte[] webp=java.util.Base64.getDecoder().decode("UklGRjwAAABXRUJQVlA4IDAAAADQAQCdASoFAAMAAUAmJaACdLoB+AADsAD+8ut//NgVzXPv9//S4P0uD9Lg/9KQAAA=");
        UploadedImages.Image image=UploadedImages.sanitize(webp);
        BufferedImage decoded=ImageIO.read(new ByteArrayInputStream(image.bytes));
        assertEquals(5,decoded.getWidth());assertEquals(3,decoded.getHeight());
    }
}

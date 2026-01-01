package com.appalanche.backend.logos;

import com.appalanche.backend.logos.business.CompanyLogoService;
import com.appalanche.backend.logos.persistence.CompanyLogo;
import com.appalanche.backend.logos.persistence.CompanyLogoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static java.time.Instant.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureMockRestServiceServer
class CompanyLogoServiceTests {

    @Autowired
    private CompanyLogoService logoService;

    @Autowired
    private CompanyLogoRepository repository;

    @Autowired
    private MockRestServiceServer mockServer;

    @TestConfiguration
    static class TestConfig {

        @Bean("logoClient")
        @Primary
        public RestClient testLogoClient(RestClient.Builder builder) {
            return builder.baseUrl("https://img.logo.dev").build();
        }
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mockServer.reset();
    }

    @Test
    void shouldFetchFromApiUsingFullDomainSearchIfCacheMiss() {
        String brand = "google";
        String tld = "com";
        byte[] fetchedImage = new byte[]{1, 2, 3, 4};
        mockServer.expect(requestTo(containsString(toDomain(brand, tld))))
                  .andRespond(withSuccess(fetchedImage, IMAGE_JPEG));

        CompanyLogo result = logoService.getCompanyLogo(brand, tld);

        assertThat(result.getImageBytes()).isEqualTo(fetchedImage);
        assertThat(repository.count()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    void shouldFetchFromDatabaseUsingFullDomainSearchIfCacheHit() {
        String brand = "amazon";
        String tld = "com";
        byte[] cachedImage = new byte[]{9, 9, 9};
        CompanyLogo existing = createDomainLogoNow(brand, tld, cachedImage, 9, "image/jpeg");
        repository.save(existing);

        CompanyLogo result = logoService.getCompanyLogo(brand, tld);

        assertThat(result.getImageBytes()).isEqualTo(cachedImage);
        mockServer.verify();
    }

    @Test
    void shouldFetchFromApiUsingBrandSearchIfCacheMiss() {
        String brand = "google";
        byte[] fetchedImage = new byte[]{1, 2, 3, 4};
        mockServer.expect(requestTo(containsString(brand))).andRespond(withSuccess(fetchedImage, IMAGE_JPEG));

        CompanyLogo result = logoService.getCompanyLogo(brand);

        assertThat(result.getImageBytes()).isEqualTo(fetchedImage);
        assertThat(repository.count()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    void shouldFetchFromDatabaseUsingBrandSearchIfCacheHit() {
        String brand = "amazon";
        byte[] cachedImage = new byte[]{9, 9, 9};
        CompanyLogo existing = createBrandLogoNow(brand, cachedImage, 9, "image/jpeg");
        repository.save(existing);

        CompanyLogo result = logoService.getCompanyLogo(brand);

        assertThat(result.getImageBytes()).isEqualTo(cachedImage);
        mockServer.verify();
    }

    @Test
    void shouldRefetchFromApiUsingFullDomainSearchIfCacheHitButSizeMismatch() {
        String brand = "google";
        String tld = "com";
        byte[] cachedImage = new byte[]{9, 9, 9};
        byte[] fetchedImage = new byte[]{1, 2, 3, 4};
        var invalidSize = 0;
        CompanyLogo existing = createDomainLogoNow(brand, tld, cachedImage, invalidSize, "image/jpeg");
        repository.save(existing);
        mockServer.expect(requestTo(containsString(toDomain(brand, tld))))
                  .andRespond(withSuccess(fetchedImage, IMAGE_JPEG));

        CompanyLogo result = logoService.getCompanyLogo(brand, tld);

        assertThat(result.getImageBytes()).isEqualTo(fetchedImage);
        assertThat(repository.count()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    void shouldRefetchFromApiUsingBrandSearchIfCacheHitButSizeMismatch() {
        String brand = "amazon";
        byte[] cachedImage = new byte[]{9, 9, 9};
        byte[] fetchedImage = new byte[]{1, 2, 3, 4};
        var invalidSize = 0;
        CompanyLogo existing = createBrandLogoNow(brand, cachedImage, invalidSize, "image/jpeg");
        repository.save(existing);
        mockServer.expect(requestTo(containsString(brand))).andRespond(withSuccess(fetchedImage, IMAGE_JPEG));

        CompanyLogo result = logoService.getCompanyLogo(brand);

        assertThat(result.getImageBytes()).isEqualTo(fetchedImage);
        assertThat(repository.count()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    void shouldRefetchFromApiUsingFullDomainSearchIfCacheHitButContentTypeMismatch() {
        String brand = "google";
        String tld = "com";
        byte[] cachedImage = new byte[]{9, 9, 9};
        byte[] fetchedImage = new byte[]{1, 2, 3, 4};
        var nonConfiguredContentType = "image/png";
        CompanyLogo existing = createDomainLogoNow(brand, tld, cachedImage, 9, nonConfiguredContentType);
        repository.save(existing);
        mockServer.expect(requestTo(containsString(toDomain(brand, tld))))
                  .andRespond(withSuccess(fetchedImage, IMAGE_JPEG));

        CompanyLogo result = logoService.getCompanyLogo(brand, tld);

        assertThat(result.getImageBytes()).isEqualTo(fetchedImage);
        assertThat(repository.count()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    void shouldRefetchFromApiUsingBrandSearchIfCacheHitButContentTypeMismatch() {
        String brand = "amazon";
        byte[] cachedImage = new byte[]{9, 9, 9};
        byte[] fetchedImage = new byte[]{1, 2, 3, 4};
        var nonConfiguredContentType = "image/png";
        CompanyLogo existing = createBrandLogoNow(brand, cachedImage, 9, nonConfiguredContentType);
        repository.save(existing);
        mockServer.expect(requestTo(containsString(brand))).andRespond(withSuccess(fetchedImage, IMAGE_JPEG));

        CompanyLogo result = logoService.getCompanyLogo(brand);

        assertThat(result.getImageBytes()).isEqualTo(fetchedImage);
        assertThat(repository.count()).isEqualTo(1);
        mockServer.verify();
    }

    private static String toDomain(String brand, String tld) {
        return brand + '.' + tld;
    }

    private static CompanyLogo createDomainLogoNow(String brand, String tld, byte[] image, int size, String contentType) {
        return new CompanyLogo(brand, tld, image, size, contentType, now());
    }

    private static CompanyLogo createBrandLogoNow(String brand, byte[] image, int size, String contentType) {
        return new CompanyLogo(brand, "", image, size, contentType, now());
    }
}

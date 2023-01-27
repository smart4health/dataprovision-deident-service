package com.healthmetrix.deident.uploader

import com.healthmetrix.deident.commons.SecretKey
import com.healthmetrix.deident.commons.Secrets
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec

@Configuration
@Profile("remote-rp")
class WebClientConfiguration {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Bean
    fun provideCertificateFactory(): CertificateFactory = CertificateFactory.getInstance("X.509")

    @Bean
    fun provideKeyFactory(): KeyFactory = KeyFactory.getInstance("RSA")

    @Bean("serverCert")
    fun provideServerCert(secrets: Secrets, certificateFactory: CertificateFactory) =
        secrets[SecretKey.RP_SERVER_CERT]
            .let(String::byteInputStream)
            .let(certificateFactory::generateCertificate) as X509Certificate

    @Bean("clientCert")
    fun provideClientCert(secrets: Secrets, certificateFactory: CertificateFactory) =
        secrets[SecretKey.RP_CLIENT_CERT]
            .let(String::byteInputStream)
            .let(certificateFactory::generateCertificate) as X509Certificate

    @Bean
    fun provideClientKey(secrets: Secrets, keyFactory: KeyFactory): PrivateKey =
        secrets[SecretKey.RP_CLIENT_KEY]
            .byteInputStream()
            .reader()
            .let(::PemReader)
            .readPemObject()
            .content
            .let(::PKCS8EncodedKeySpec)
            .let(keyFactory::generatePrivate)

    @Bean
    fun provideSslContext(
        @Qualifier("serverCert")
        serverCert: X509Certificate,
        @Qualifier("clientCert")
        clientCert: X509Certificate,
        clientKey: PrivateKey,
    ): SslContext = SslContextBuilder
        .forClient()
        .trustManager(serverCert)
        .keyManager(clientKey, clientCert)
        .build()

    @Bean
    fun provideRpHttpClient(sslContext: SslContext): HttpClient =
        HttpClient.create().secure { it.sslContext(sslContext) }

    @Bean
    fun provideRpHttpConnector(httpClient: HttpClient): ClientHttpConnector =
        ReactorClientHttpConnector(httpClient)

    @Bean("rpWebClient")
    fun provideRpWebClient(clientHttpConnector: ClientHttpConnector): WebClient =
        WebClient.builder()
            .clientConnector(clientHttpConnector)
            .build()
}

@Configuration
@Profile("local-rp")
class LocalRpWebClientConfiguration {
    @Bean("rpWebClient")
    fun provideRpWebClient(): WebClient = WebClient.create()
}

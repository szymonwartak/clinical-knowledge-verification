package com.babylonhealth.util


import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

class SSL {

}

trait SSLConfiguration {

  implicit def sslContext: SSLContext = {
    val keystore = "keystore.jks"
    val password = "asdfasdf"

    val keyStore = KeyStore.getInstance("jks")
    val in = getClass.getClassLoader.getResourceAsStream(keystore)
    require(in != null, "Bad java key storage file: " + keystore)
    keyStore.load(in, password.toCharArray)

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, password.toCharArray)
    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(keyStore)
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)
    context
  }

}
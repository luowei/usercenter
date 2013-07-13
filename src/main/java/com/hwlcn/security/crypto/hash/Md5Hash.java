package com.hwlcn.security.crypto.hash;

import com.hwlcn.security.codec.Base64;
import com.hwlcn.security.codec.Hex;


public class Md5Hash extends SimpleHash {

    public static final String ALGORITHM_NAME = "MD5";

    public Md5Hash() {
        super(ALGORITHM_NAME);
    }

    public Md5Hash(Object source) {
        super(ALGORITHM_NAME, source);
    }

    public Md5Hash(Object source, Object salt) {
        super(ALGORITHM_NAME, source, salt);
    }

    public Md5Hash(Object source, Object salt, int hashIterations) {
        super(ALGORITHM_NAME, source, salt, hashIterations);
    }

    public static Md5Hash fromHexString(String hex) {
        Md5Hash hash = new Md5Hash();
        hash.setBytes(Hex.decode(hex));
        return hash;
    }

    public static Md5Hash fromBase64String(String base64) {
        Md5Hash hash = new Md5Hash();
        hash.setBytes(Base64.decode(base64));
        return hash;
    }
}

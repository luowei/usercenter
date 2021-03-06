
package com.hwlcn.security.crypto.hash.format;

import com.hwlcn.security.crypto.hash.Hash;


public class HexFormat implements HashFormat {

    public String format(Hash hash) {
        return hash != null ? hash.toHex() : null;
    }
}

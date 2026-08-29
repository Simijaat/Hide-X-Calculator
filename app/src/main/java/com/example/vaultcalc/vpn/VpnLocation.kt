package com.example.vaultcalc.vpn

enum class VpnLocation(val displayName: String, val endpoint: String = "") {
    US("United States"),
    NL("Netherlands"),
    CH("Switzerland"),
    SG("Singapore"),
    JP("Japan");

    companion object {
        fun default(): VpnLocation = US
    }
}

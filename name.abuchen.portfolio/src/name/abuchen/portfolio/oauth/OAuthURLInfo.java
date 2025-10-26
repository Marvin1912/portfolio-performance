package name.abuchen.portfolio.oauth;

import name.abuchen.portfolio.oauth.impl.PKCE;

public class OAuthURLInfo
{
    private final String authorizationUrl;
    private final String callbackUrl;
    private final PKCE pkce;
    private final String state;

    public OAuthURLInfo(String authorizationUrl, String callbackUrl, PKCE pkce, String state)
    {
        this.authorizationUrl = authorizationUrl;
        this.callbackUrl = callbackUrl;
        this.pkce = pkce;
        this.state = state;
    }

    public String getAuthorizationUrl()
    {
        return authorizationUrl;
    }

    public String getCallbackUrl()
    {
        return callbackUrl;
    }

    public PKCE getPkce()
    {
        return pkce;
    }

    public String getState()
    {
        return state;
    }
}
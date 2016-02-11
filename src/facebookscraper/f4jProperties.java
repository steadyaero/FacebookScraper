package facebookscraper;

import facebook4j.Facebook;
import facebook4j.auth.AccessToken;

public class f4jProperties 
{
	public f4jProperties(Facebook facebook)
	{
		//debug=false
		String appId="1031834396880105";
		String appSecret="b8779f789d78e0c2533dacf67d93f59b";
		String accessToken="1031834396880105|-4rzvW5aed08ZeIl5Fi4yR-p_Y4";
		facebook.setOAuthAppId(appId, appSecret);
		//facebook.setOAuthPermissions(commaSeparetedPermissions);
		facebook.setOAuthAccessToken(new AccessToken(accessToken, null));
	}
}

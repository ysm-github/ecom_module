import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.sxit.crawler.utils.RegexUtils;
import com.sxit.crawler.utils.TextUtils;
import com.sxit.crawler.utils.UrlUtils;



/**
 * <strong>RegexTest</strong><br>
 * <p>
 *
 * </p>
 * @since 0.1
 * @version $Id: RegexTest.java,v 0.1 2013-10-16 上午11:59:32 lex Exp $
 */
public class RegexTest {

	public static void main(String[] args) throws Exception{
		File f = new File("e:/b.htm");
//		File f = new File("e:/c.htm");
//		File f = new File("e:/d.htm");
		String str = FileUtils.readFileToString(f, "GB2312");
//		String s = TextUtils.extrValueByRegx("<span[\\s\\S]*?class=\"shop-count\"[\\s\\S]*?>[\\s\\S]*?<b>([\\d]*?)</b>[\\s\\S]*?</span>", str, 1);
//		System.out.println(s);
		
//		String url = "http://shop35668351.taobao.com/search.htm?search=y";
//		String str = "<span class=\"page-info\">3/21</span>";
//		String s = TextUtils.extrValueByRegx("<span[\\s\\S]*?class=\"page-info\"[\\s\\S]*?>.*/([\\d]*?)</span>", str, 1);
//		System.out.println(s);
//		String str = RegexUtils.testRegex(url, "<meta[\\s\\S]*?name=\"microscope\\-data\"[\\s\\S]*?content=\"(.*)\"[\\s\\S]*?/>", 1);
//		String shopId = TextUtils.extrValueByRegx("shopId=([\\d]*?);", str, 1);
//		String userId = TextUtils.extrValueByRegx("userId=([\\d]*?)[\\s]*?", str, 1);
		String shopId = TextUtils.extrValueByRegx("\"shopId\"[\\s\\S]*?:[\\s\\S]*?\"([\\d]*?)\"", str, 1);
		String userId = TextUtils.extrValueByRegx("\"userId\"[\\s\\S]*?:[\\s\\S]*?\"([\\d]*?)\"", str, 1);
		String userNick = TextUtils.extrValueByRegx("\"user_nick\"[\\s\\S]*?:[\\s\\S]*?\"([\\S]*?)\"", str, 1);
		String alisHref = TextUtils.extrValueByRegx("<a class=\"hCard[\\s\\S]*?\" href=\"([\\S]*?)\">", str, 1);
		if (StringUtils.isNotBlank(userNick)) {
			userNick = UrlUtils.decodeUrl(userNick, "utf-8");
		}
		System.out.println("shopId:"+shopId);
		System.out.println("userId:"+userId);
		System.out.println("userNick:"+userNick);
		System.out.println("alisHref:"+alisHref);
		List<String> itemHrefs = TextUtils.extrValuesByRegx("href=\"http\\://item\\.taobao\\.com/item\\.htm\\?id=([\\d]*?)\"", str, 1);
		for (String itemHref : itemHrefs) {
			System.out.println(itemHref);
		}
		
		String testStr = "http://item.taobao.com/item.htm?xxx&id=19118890600&";
		String s = TextUtils.extrValueByRegx("id=([\\d]*)", testStr, 1);
		System.out.println("---------------------");
		System.out.println(s);
		
	}
}

package cricscore.servlet;



import cricscore.models.Match;
import cricscore.models.SimpleScore;
import cricscore.service.CricScoreService;
import cricscore.service.JSONHelperService;
import cricscore.utils.StringUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class CricScoreServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger
			.getLogger(CricScoreServlet.class.getName());
	private CricScoreService cricScoreService;
	private JSONHelperService jsonHelperService;

	public CricScoreServlet() {
		cricScoreService = new CricScoreService();
		jsonHelperService = new JSONHelperService();
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		
		String responseString = null;
		String paramId = (String) request.getParameter("id");
        if (paramId != null) {
            paramId = URLDecoder.decode(paramId);
        }
		logger.info("ID = " + paramId);
		if (paramId == null) {
			List<Match> matches = cricScoreService.getMatches();
			responseString = jsonHelperService.getMatchesJSON(matches);
		} else if (StringUtils.isNumericPlus(paramId)) {
			long timestamp = 0;
			Date lastModified = StringUtils.getDate(request.getHeader("If-Modified-Since"));
			logger.info("lastModified = " + lastModified);
			if(lastModified != null){
				timestamp = lastModified.getTime();
			}
			String[] matchIds = paramId.split(" ");
			logger.finer("Request matches : " + matchIds);
			List<SimpleScore> scores = new ArrayList<SimpleScore>();
			for (String matchId : matchIds) {
				int id = Integer.parseInt(matchId);
				SimpleScore simpleScore = cricScoreService.getScore(id);
				if(timestamp == 0 || (simpleScore != null && simpleScore.getTimestamp() > timestamp)){
					scores.add(simpleScore);
				}
			}
			if(scores.size() == 0){
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;	
			}
			responseString = jsonHelperService.getSimpleScoresJSON(scores, System.currentTimeMillis());
			
		} else {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if (responseString != null) {
			logger.finer("responseString: " + responseString);	
			response.setContentType("application/json");
			response.addHeader("Last-Modified", StringUtils.getDate(new Date()));
			response.getWriter().println(responseString);
		}
	}
}
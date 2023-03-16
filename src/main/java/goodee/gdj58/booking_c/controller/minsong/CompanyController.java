package goodee.gdj58.booking_c.controller.minsong;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import goodee.gdj58.booking_c.service.minsong.CompanyService;
import goodee.gdj58.booking_c.util.FontColor;
import goodee.gdj58.booking_c.vo.Company;
import goodee.gdj58.booking_c.vo.CompanyDetail;
import goodee.gdj58.booking_c.vo.CompanyOffday;
import goodee.gdj58.booking_c.vo.CompanyType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class CompanyController {
	@Autowired
	private CompanyService companyService;
	private String[] timetable = {"00:00", "00:30", "01:00", "01:30", "02:00", "02:30", "03:00", "03:30", "04:00", "04:30", "05:00", "05:30", "06:00", "06:30", "07:00", "07:30", "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30"};
	private String[] addtionalService = {"주차 가능", "제로페이", "배달", "포장"};
	
	// 업체 등록
	@GetMapping("/company/addCompanyDetail")
	public String addCompanyDetail(Model model) {
		List<CompanyType> typeList = companyService.getCompanyTypeList();
		log.debug(FontColor.PURPLE+"===============>"+typeList);
		
		model.addAttribute("typeList", typeList);
		model.addAttribute("timetable", timetable);
		model.addAttribute("addtionalService", addtionalService);
		model.getAttribute("msg");
		return "companyDetail/addCompanyDetail";
	}
	@PostMapping("/company/addCompanyDetail")
	public String addCompanyDetail(HttpSession session, CompanyDetail companyDetail
									, @RequestParam(value="am_pm") String[] am_pm
									, @RequestParam(value="companyOffdayDate", required = false) String[] companyOffdayDate
									, @RequestParam(value="companyOffdayMemo", required = false) String[] companyOffdayMemo
									, @RequestParam(value="dayofweek", required = false) int[] dayofweek) {
		// 1. CompanyDetail
		// 아이디
		Company loginCompany = (Company)session.getAttribute("loginCompany");
		String loginCompanyId = loginCompany.getCompanyId();
		companyDetail.setCompanyId(loginCompanyId);
		
//		// 유형 이름으로 변경
//		companyDetail.setCompanyTypeContent(companyService.getcompanyTypeContent(Integer.parseInt(companyDetail.getCompanyTypeContent())));
		
		// 오후 판별
		String[] time = {companyDetail.getOpenTime(), companyDetail.getCloseTime()};
		for(int i = 0; i < 2; i++) {
			if(am_pm[i].equals("pm")) {
				StringBuilder sb = new StringBuilder();
				int hour = Integer.parseInt(time[i].substring(0, 2))+12;
				log.debug(FontColor.PURPLE+"========시간 자르기=======>"+hour);
				sb = sb.append(hour).append(time[i].substring(2));
				if(i == 0) {					
					companyDetail.setOpenTime(sb+"");
				}else {
					companyDetail.setCloseTime(sb+"");
				}
			}
		}
		
		// 부가 서비스 해당 없음
		if(companyDetail.getAdditionService() == null) {
			companyDetail.setAdditionService("해당없음");
		}
		
		log.debug(FontColor.PURPLE+"========개장======>"+companyDetail.getOpenTime());
		log.debug(FontColor.PURPLE+"========마감=======>"+companyDetail.getCloseTime());
		log.debug(FontColor.PURPLE+"========부가서비스=======>"+companyDetail.getAdditionService());
		
		int row = companyService.addCompanyDetail(companyDetail);
		
		if(row != 1) {
			return "company/addCompanyDetail";
		}
		
		// 2. CompanyOffday
		row = 0;

		CompanyOffday companyOffday = new CompanyOffday();
		companyOffday.setCompanyId(loginCompanyId);

		if(companyOffdayDate != null) {			
			for(int i = 0; i < companyOffdayDate.length; i++) {
				if(!companyOffdayDate[i].equals("")) {
					companyOffday.setCompanyOffdayDate(companyOffdayDate[i]);
					companyOffday.setCompanyOffdayMemo(companyOffdayMemo[i]);
					row += companyService.addCompanyOffday(companyOffday);
				}
			}
			
			if(row == 0) {
				return "company/addCompanyDetail";
			}
		}
		
		// 요일별
		if(dayofweek != null) {			
			for(int i = 0; i < dayofweek.length; i++) {
				// 해당 요일의 날짜 구하기
				String date = companyService.getCompanyOffdayOfWeek(dayofweek[i]);
				log.debug(FontColor.PURPLE+date+"<--------");
				// 쿼리 돌리기
				for(int j = 0; j < 365; j+=7) {
					log.debug(FontColor.PURPLE+j+"요일 계산");
					if(companyService.countOffday(loginCompanyId, companyOffdayDate[i]) == 0) {	// 앞에서 등록한 사유가 있는 날이면 제외
						companyService.addCompanyOffdayOfWeek(loginCompanyId, date, j);						
					}
				}
			}
		}
			
		return "redirect:/company/addCompanyDetail";
	}
	
	// 업체 수정
	@GetMapping("/company/modifyCompanyDetail")
	public String modifyCompanyDetail(HttpSession session,Model model) {
		// 세션에서 아이디 불러오기
		Company loginCompany = (Company)session.getAttribute("loginCompany");
		String companyId = loginCompany.getCompanyId();
		
		// 기존 업체 정보
		CompanyDetail companyDetail = companyService.getCompanyDetail(companyId);
		// 오후 반영
		String[] time = {companyDetail.getOpenTime().substring(0, 5), companyDetail.getCloseTime().substring(0, 5)};
		String[] ampm = {"am", "am"};
		
		for(int i = 0; i < 2; i++) {
			if(Integer.parseInt(time[i].substring(0, 2)) > 12) {
				ampm[i] = "pm";
				StringBuilder sb = new StringBuilder();
				String hour = (Integer.parseInt(time[i].substring(0, 2))-12)+"";
				if(Integer.parseInt(hour) < 10) {
					hour = "0"+hour;
				}
				log.debug(FontColor.PURPLE+"========시간 자르기=======>"+hour);
				sb = sb.append(hour).append(time[i].substring(2));
				
				time[i] = sb+"";
			}
		}
		log.debug(FontColor.PURPLE+time[0]+", "+time[1]+":::::::::::::::시간");
		log.debug(FontColor.PURPLE+ampm[0]+", "+ampm[1]+":::::::::::::::오전오후");
		
		// 예약 정보
		Set<String> bookingDate = companyService.getBookingDate(companyId);
//		List<String> bookingDate = companyService.getBookingDate(companyId);
		log.debug(FontColor.PURPLE+bookingDate+"<=======예약일자 목록");

		model.addAttribute("ampm", ampm);
		model.addAttribute("openTime", time[0]);
		model.addAttribute("closeTime", time[1]);
		model.addAttribute("additionService", companyDetail.getAdditionService());
		model.addAttribute("timetable", timetable);
		model.addAttribute("addtionalService", addtionalService);
//		model.addAttribute("bookingDate", bookingDate);
		model.getAttribute("msg");
		return "companyDetail/modifyCompanyDetail";
	}
	@PostMapping("/company/modifyCompanyDetail")
	public String modifyCompanyDetail(HttpSession session, CompanyDetail companyDetail
			, @RequestParam(value="am_pm") String[] am_pm
			, @RequestParam(value="companyOffdayDate", required = false) String[] companyOffdayDate
			, @RequestParam(value="companyOffdayMemo", required = false) String[] companyOffdayMemo
			, @RequestParam(value="dayofweek", required = false) int[] dayofweek
			, @RequestParam(value="companyWorkingdayDate", required = false) String[] companyWorkingdayDate){
	// 1. CompanyDetail
	// 아이디
	Company loginCompany = (Company)session.getAttribute("loginCompany");
	String loginCompanyId = loginCompany.getCompanyId();
	companyDetail.setCompanyId(loginCompanyId);
	
	// 오후 판별
	String[] time = {companyDetail.getOpenTime(), companyDetail.getCloseTime()};
	for(int i = 0; i < 2; i++) {
		if(am_pm[i].equals("pm")) {
			StringBuilder sb = new StringBuilder();
			int hour = Integer.parseInt(time[i].substring(0, 2))+12;
			log.debug(FontColor.PURPLE+"========시간 자르기=======>"+hour);
			sb = sb.append(hour).append(time[i].substring(2));
			if(i == 0) {					
				companyDetail.setOpenTime(sb+"");
			}else {
				companyDetail.setCloseTime(sb+"");
			}
		}
	}
	
	// 부가 서비스 해당 없음
	if(companyDetail.getAdditionService() == null) {
		companyDetail.setAdditionService("해당없음");
	}
	
	log.debug(FontColor.PURPLE+"========개장======>"+companyDetail.getOpenTime());
	log.debug(FontColor.PURPLE+"========마감=======>"+companyDetail.getCloseTime());
	log.debug(FontColor.PURPLE+"========부가서비스=======>"+companyDetail.getAdditionService());
	
	int row = companyService.modifyCompanyDetail(companyDetail);
	
	if(row != 1) {
		return "company/modifyCompanyDetail";
	}
	
	// 2. CompanyOffday
	
	CompanyOffday companyOffday = new CompanyOffday();
	companyOffday.setCompanyId(loginCompanyId);
	
	
	// 휴무일 삭제 먼저 하고 휴무일 추가 (기존 휴무일 고려, 새로운 사유로 갱신하기 위함)
	// 1-1. 휴무일 삭제(개별)
	row = 0;
	if(companyWorkingdayDate != null) {			
		for(int i = 0; i < companyWorkingdayDate.length; i++) {
			if(!companyWorkingdayDate[i].equals("")) {
				row += companyService.removeOffday(loginCompanyId, companyWorkingdayDate[i], 0);
			}
		}
		
		if(row == 0) {
			return "company/modifyCompanyDetail";
		}
	}
	
	// 1-2. 휴무일 삭제(요일)
	row = 0;
	for(int i = 1; i < 8; i++) {
		int num = i;
		if(!IntStream.of(dayofweek).anyMatch(x -> x == num)) {	// 체크 안 돼 있으면
			String date = companyService.getCompanyOffdayOfWeek(dayofweek[i]);
			// 쿼리 돌리기
			for(int j = 0; j < 365; j+=7) {
				log.debug(FontColor.PURPLE+j+"요일 계산");
				companyService.removeOffday(loginCompanyId, date, j);					
			}
		}
	}

	// 2-1. 휴무일 추가(개별)
	row = 0;
	if(companyOffdayDate != null) {			
		for(int i = 0; i < companyOffdayDate.length; i++) {
			if(!companyOffdayDate[i].equals("") && companyService.countOffday(loginCompanyId, companyOffdayDate[i]) == 0) {	// 이전에 등록된 날 제외
				companyOffday.setCompanyOffdayDate(companyOffdayDate[i]);
				companyOffday.setCompanyOffdayMemo(companyOffdayMemo[i]);
				row += companyService.addCompanyOffday(companyOffday);
			}
		}
		
		if(row == 0) {
			return "company/modifyCompanyDetail";
		}
	}
	
	// 2-2. 휴무일 추가(요일)
	row = 0;
	if(dayofweek != null) {			
		for(int i = 0; i < dayofweek.length; i++) {
			// 해당 요일의 날짜 구하기
			String date = companyService.getCompanyOffdayOfWeek(dayofweek[i]);
			log.debug(FontColor.PURPLE+date+"<--------");
			// 쿼리 돌리기
			for(int j = 0; j < 365; j+=7) {
				log.debug(FontColor.PURPLE+j+"요일 계산");
				if(companyService.countOffday(loginCompanyId, companyOffdayDate[i]) == 0) {	// 앞에서 등록한 사유가 있는 날이면 제외
					companyService.addCompanyOffdayOfWeek(loginCompanyId, date, j);						
				}
			}
		}
		
		if(row == 0) {
			return "company/modifyCompanyDetail";
		}
	}
	
	return "redirect:/company/addCompanyDetail";
	}
}

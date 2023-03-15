package goodee.gdj58.booking_c.restcontroller.taehee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import goodee.gdj58.booking_c.service.taehee.CompanyService3;
import goodee.gdj58.booking_c.vo.Company;
import goodee.gdj58.booking_c.vo.CompanyOffday;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ProductRestController {
	@Autowired CompanyService3 companyService;

	// 기업 휴무일
	@GetMapping("/companyOffday")
	public List<Map<String, Object>> getCompanyOffday(HttpSession session) {
		Company loginCom = (Company)session.getAttribute("loginCompany");
		List<Map<String, Object>> getOffday = companyService.getOffday(loginCom.getCompanyId());
		List<Map<String, Object>> list = new ArrayList<>();
		for(Map<String, Object> off : getOffday) {
			Map<String, Object> map = new HashMap<>();
			map.put("title", off.get("memo"));
			map.put("start", off.get("offday"));
			list.add(map);
		}
		
		return list;
	}
}

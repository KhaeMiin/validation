package hello.itemservice.web.validation;

import hello.itemservice.domain.item.Item;
import hello.itemservice.domain.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/validation/v2/items")
@RequiredArgsConstructor
public class ValidationItemControllerV2 {

    private final ItemRepository itemRepository;

    @GetMapping
    public String items(Model model) {
        List<Item> items = itemRepository.findAll();
        model.addAttribute("items", items);
        return "validation/v2/items";
    }

    @GetMapping("/{itemId}")
    public String item(@PathVariable long itemId, Model model) { //@PathVariable(경로 변수) 사용 (@GetMapping("/{itemId}")에서 {itemId} 이거
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/item";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("item", new Item());
        return "validation/v2/addForm";
    }


    /**
     * 한번 더 복습 겸!!
     * String , int , Integer 같은 단순 타입 = @RequestParam
     * 나머지 = @ModelAttribute (argument resolver 로 지정해둔 타입 외)
     *
     * ※ BindingResult 주의!!!
     * BindingResult 파라미터의 위치는 반드시 @ModelAttribute Object object 다음에 와야한다.
     *
     * new FieldError(String objectName, String fieldId, String defaultMessage)
     * objectName: @ModelAttribute 이름
     * field: 오류가 발생한 필드 이름
     * defaultMessage: 오류 기본 메시지
     *
     * 정리:
     * 필드에 오류가 있으면 'FieldError 객체를 생성해서 BindingResult에 담아두면 된다.
     *
     * ※필드가 없을 경우는??(GlobalError)
     * ObjectError 객체를 생성해서 BingingResult 담아두면 된다.
     * new ObjectError(String objectName, String defaultMessage)
     * objectName: @ModelAttribute 이름
     * defaultMessage: 오류 기본 메시지
     */
    @PostMapping("/add")
    public String addItemV1(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) { //@ModelAttribute: 생략가능


        //검증 로직
        /**
         * StringUtils 클래스의 hasText(String) 메소드는
         * null 체크, 길이가 0보다 큰지 체크, 공백이 아닌 문자열이 하나라도 포함되었는지까지 한번에 검증해준다!
         */
        if (!StringUtils.hasText(item.getItemName())) {
            bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수입니다."));
        }
        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000까지 허용합니다."));

        }
        if (item.getQuantity() == null || item.getQuantity() >= 9999) {
            bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999까지 허용합니다"));
        }

        //특정 필드가 아닌 복합 룰 검증 (GlobalError)
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                bindingResult.addError(new ObjectError("item", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
            }
        }

        //검증에 실패하면 다시 입력 폼으로
        if (bindingResult.hasErrors()) {//bindingResult.에러가 있으면
            log.info("errors = {}", bindingResult);//model에 자동으로 담긴다.
            return "validation/v2/addForm";//파라미터에서 받은 item 객체가 model에 담기기 때문에 작성한 데이터 유지
        }

        //성공 로직

        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true); //쿼리스트링으로 넘어감
        return "redirect:/validation/v2/items/{itemId}";
    }

    @GetMapping("/{itemId}/edit")
    public String editForm(@PathVariable Long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/editForm";
    }

    @PostMapping("/{itemId}/edit")
    public String edit(@PathVariable Long itemId, @ModelAttribute Item item) {
        itemRepository.update(itemId, item);
        return "redirect:/validation/v2/items/{itemId}";
    }

}


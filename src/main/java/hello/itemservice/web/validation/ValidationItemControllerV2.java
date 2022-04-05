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
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
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
    private final ItemValidator itemValidator;

    @InitBinder// 컨트롤러가 호출될 때 마다 만들어짐
    public void init(WebDataBinder dataBinder) {
        dataBinder.addValidators(itemValidator);
    }

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
     *
     *
     * 문제점: 에러가 난 필드의 입력한 값이 사라짐짐
     * 예: 가격을 1000원 미만으로 입력을 해도 입력한 값이 남아있어야하는데 유지되지 않음
    */
//    @PostMapping("/add")
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

    /**
     *new FieldError()
     *파라미터 목록
     * objectName : 오류가 발생한 객체 이름
     * field : 오류 필드
     * rejectedValue : 사용자가 입력한 값(거절된 값)
     * bindingFailure : 타입 오류 같은 바인딩 실패인지, 검증 실패인지 구분 값
     * codes : 메시지 코드
     * arguments : 메시지에서 사용하는 인자
     * defaultMessage : 기본 오류 메시지
     */
//    @PostMapping("/add")
    public String addItemV2(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) { //@ModelAttribute: 생략가능


        //검증 로직
        /**
         * StringUtils 클래스의 hasText(String) 메소드는
         * null 체크, 길이가 0보다 큰지 체크, 공백이 아닌 문자열이 하나라도 포함되었는지까지 한번에 검증해준다!
         */
        if (!StringUtils.hasText(item.getItemName())) {
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, null, null, "상품 이름은 필수입니다."));
        }
        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, null, null, "가격은 1,000 ~ 1,000,000까지 허용합니다."));

        }
        if (item.getQuantity() == null || item.getQuantity() >= 9999) {
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, null, null, "수량은 최대 9,999까지 허용합니다"));
        }

        //특정 필드가 아닌 복합 룰 검증 (GlobalError)
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                bindingResult.addError(new ObjectError("item", null, null,  "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
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


    /**
     *new FieldError()
     *파라미터 목록
     * objectName : 오류가 발생한 객체 이름
     * field : 오류 필드
     * rejectedValue : 사용자가 입력한 값(거절된 값)
     * bindingFailure : 타입 오류 같은 바인딩 실패인지, 검증 실패인지 구분 값
     * codes : 메시지 코드
     * arguments : 메시지에서 사용하는 인자
     * defaultMessage : 기본 오류 메시지
     *
     * FieldError , ObjectError 의 생성자는 errorCode , arguments 를 제공한다. 이것은 오류 발생시 오류
     * 코드로 메시지를 찾기 위해 사용된다
     */
//    @PostMapping("/add")
    public String addItemV3(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) { //@ModelAttribute: 생략가능

        //검증 로직
        /**
         * StringUtils 클래스의 hasText(String) 메소드는
         * null 체크, 길이가 0보다 큰지 체크, 공백이 아닌 문자열이 하나라도 포함되었는지까지 한번에 검증해준다!
         */
        if (!StringUtils.hasText(item.getItemName())) {
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, new String[] {"required.item.itemName", "required.default"}, null, null));
        }
        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, new String[] {"range.item.price"}, new Object[]{1000, 1000000}, null));

        }
        if (item.getQuantity() == null || item.getQuantity() >= 9999) {
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, new String[]{"max.item.quantity"}, new Object[]{9999}, null));
        }

        //특정 필드가 아닌 복합 룰 검증 (GlobalError)
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                bindingResult.addError(new ObjectError("item", new String[]{"totalPriceMin"}, new Object[]{10000, resultPrice}, null));
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

    /**
     * new FieldError(), new ObjectError 파라미터 값 넘기기가 너무 부담스럽다.
     * rejectValue(field의 경우), reject(Object경우)를 이용해서 코드를 더 간추려보자.
     * 컨트롤러에서 BindingResult 는 검증해야 할 객체인 target 바로 다음에 온다. 따라서
     * BindingResult 는 이미 본인이 검증해야 할 객체인 target 을 알고 있다
     *
     * rejectValue(fieldName, 에러코드, default 경우 메시지(null사용 가능))
     * rejectValue(fieldName, 에러코드, 에러코드에 넘겨줄 파라미터 값({0},{1}), default 경우 메시지(null사용 가능))
     * reject(에러코드, 에러코드에 넘겨줄 파라미터 값, default 경우 메시지(null사용 가능))
     *
     * errorCode 경우 errors.properties 에 있는 코드를 직접 입력하지 않았는데 작동하는 이유
     *
     */
//    @PostMapping("/add")
    public String addItemV4(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) { //@ModelAttribute: 생략가능

        log.info("objectName={}", bindingResult.getObjectName());
        log.info("target={}", bindingResult.getTarget());

        //검증 로직

        // 단순한 기능 제공시 아래와 같이 if문이 아닌 한줄로 사용 가능하다.
        ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "itemName", "required");
//        if (!StringUtils.hasText(item.getItemName())) {
//            bindingResult.rejectValue("itemName", "required", null);
//        }

        /**
         * StringUtils 클래스의 hasText(String) 메소드는
         * null 체크, 길이가 0보다 큰지 체크, 공백이 아닌 문자열이 하나라도 포함되었는지까지 한번에 검증해준다!
         */
        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.rejectValue("price", "range", new Object[]{1000, 1000000}, null);

        }
        if (item.getQuantity() == null || item.getQuantity() >= 9999) {
            bindingResult.rejectValue("quantity", "max", new Object[]{9999}, "기본 오류메시지 생략가능");
        }

        //특정 필드가 아닌 복합 룰 검증 (GlobalError)
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                bindingResult.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
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

//    @PostMapping("/add")
    public String addItemV5(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) { //@ModelAttribute: 생략가능

        itemValidator.validate(item, bindingResult);

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

    /**
     * @Validated
     * 메소드 호출시 item에 validator 적용 (직접 validator를 호출하지 않아도 됨)
     */
    @PostMapping("/add")
    public String addItemV6(@Validated @ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) { //@ModelAttribute: 생략가능


        //검증에 실패하면 다시 입력 폼으로
        if (bindingResult.hasErrors()) {//bindingResult.에러가 있으면
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


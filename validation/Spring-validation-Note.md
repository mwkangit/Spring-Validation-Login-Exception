# Practice Setting



**Project : validation**

```groovy
plugins {
	id 'org.springframework.boot' version '2.4.4'
	id 'io.spring.dependency-management' version '1.0.11.RELEASE'
	id 'java'
}

group = 'hello'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
	useJUnitPlatform()
}
```

- #### Validation 오류 처리에 대한 내용이다. 즉, 컨트롤러가 HTTP 요청이 정상인지 검증한다.

- #### 클라이언트 검증은 조작할 수 있으므로 보안에 취약하지만 빠르다.

- #### 서버 검증은 보안이 좋지만 느리다. 하지만 보안적인 요소로 필수다.

- #### API 방식을 사용하면 API 스펙을 잘 정의해서 검증 오류를 API 응답 결과에 잘 남겨주어야 한다.

- #### HTML은 모두 addForm.html이다.



# Validation



## Validation Task



## ![Validation Task](/media/mwkang/Klevv/Spring 일지/MVC2/03.13/Validation Task.png)

- #### 상품명 이렵하지 않거나, 가격, 수량 등이 너무 작거나 클 때 서버 검증 로직이 실패해야 한다. 이때 다시 고객에게 상품 등록 폼을 보여주고 어떠한 값이 잘못 입력되었는지 알려주어야 한다.



## V1



```java
@PostMapping("/add")
public String addItem(@ModelAttribute Item item, RedirectAttributes redirectAttributes, Model model) {

    // 검증 오류 결과를 보관
    Map<String, String> errors = new HashMap<>();

    // 검증 로직
    // 하나하나 검증하는 로직
    if(!StringUtils.hasText(item.getItemName())){
        errors.put("itemName", "상품 이름은 필수입니다.");
    }
    if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000){
        errors.put("price", "가격은 1,000 ~ 1,000,000 까지 허용합니다.");
    }
    if(item.getQuantity() == null || item.getQuantity() >= 9999){
        errors.put("quantity", "수량은 최대 9,999 까지 허용합니다.");
    }

    // 특정 필드가 아닌 복합 룰 검증
    // 가격 * 수량의 합은 10,000 원 이상직
    if (item.getPrice() != null && item.getQuantity() != null){
        int resultPrice = item.getPrice() * item.getQuantity();
        if(resultPrice < 10000){
            errors.put("globalError", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice);
        }
    }

    // 검증에 실패하면 다시 입력 폼으로
    if (!errors.isEmpty()){
        log.info("errors = {}", errors);
        model.addAttribute("errors", errors);
        return "validation/v1/addForm";
    }

    // 성공 로직
    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v1/items/{itemId}";
}
```

- #### ModelAttribute로 이전 입력한 값을 다시 출력한다. 즉, 검증 실패를 염두하여 동일한 객체를 넘기는 것이 좋다.

- #### `errors` 에 어떤 거증에서 오류가 발생했는지 정보를 담아둔다.



```html
<div th:if="${errors?.containsKey('globalError')}">
    <p class="field-error" th:text="${errors['globalError']}">전체 오류 메시지</p>
</div>

<input type="text" id="itemName" th:field="*{itemName}" class="form-control"
       th:class="${errors?.containsKey('itemName')} ? 'form-control field-error' : 'form-control'"
       placeholder="이름을 입력하세요">

<div class="field-error" th:if="${errors?.containsKey('itemName')}" th:text="${errors.itemName}">
    상품명 오류
</div>
```

- #### `th:if` 로 조거에 만졸할 때만 해당 태그를 출력한다.

- #### `?` 는 null일 경우 무시하도록 하는 것이다. 없으면 NullPointerException이 발생하는데 이것 대신 null을 반환하여 `th:if`가 null을 받아서 실패로 처리한다.

​	[SpringEL](https://docs.spring.io/spring-framework/docs/current/reference/html/
core.html#expressions-operator-safe-navigation)

- #### 조건부식을 이용하여 `form-control field-error` 을 표시할지 `form-control` 을 표시할지 결정한다.

- #### `_` 는 No-Operation으로 아무것도 표시하지 않게 할 수 있다.

- #### 오류 시 입력 폼 아래에 오류를 출력한다.

- #### 현 코드 문제점

  - #### 예상과 다른 타입이 요청될 경우 처리 어렵다. 현재 500 에러다.

  - #### 자료형 바인딩 문제를 해결해야 한다.
  
  - #### 바인딩 문제 시 컨트롤러 호출이 실패하며 오류 페이지로 이동한다.



## V2



### BindingResult V1



```java
@PostMapping("/add")
public String addItemV1(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

    // 검증 로직
    // 하나하나 검증하는 로직
    if(!StringUtils.hasText(item.getItemName())){
        bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수 입니다."));
    }
    if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000){
        bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
    }
    if(item.getQuantity() == null || item.getQuantity() >= 9999){
        bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999 까지 허용합니다."));
    }

    // 특정 필드가 아닌 복합 룰 검증
    // 가격 * 수량의 합은 10,000 원 이상직
    if (item.getPrice() != null && item.getQuantity() != null){
        int resultPrice = item.getPrice() * item.getQuantity();
        if(resultPrice < 10000){
            bindingResult.addError(new ObjectError("item", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
        }
    }

    // 검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()){
        log.info("errors = {}", bindingResult);
        return "validation/v2/addForm";
    }

    // 성공 로직
    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v2/items/{itemId}";
}
```

- #### BindingResult 파라미터는 @ModelAttribute 다음에 위치해야 한다. 즉, ModelAttribute에 바인딩된 결과값이 BindingResult에 담긴다.

- #### `FieldError` 객체는 ModelAttribute 명, 오류가 발생한 필드 이름, 오류 기본 메시지 매개변수로 이루어진다.

- #### 글로벌 에러는 `ObjectError` 로 처리한다. 매개변수로 ModelAttribute 명, 오류 기본 메시지가 있다.



```html
<div th:if="${#fields.hasGlobalErrors()}">
    <p class="field-error" th:each="err : ${#fields.globalErrors()}" th:text="${err}">글로벌 오류 메시지</p>
</div>

<input type="text" id="itemName" th:field="*{itemName}" class="form-control"
       th:errorclass="field-error"
       placeholder="이름을 입력하세요">
<div class="field-error" th:errors="*{itemName}">
    상품명 오류
</div>
```

- #### `#fields` 로 BindingResult가 제공하는 검증 오류에 접근할 수 있다.

- #### `th:errors` 로 해당 필드에 오류가 있는 경우 태그를 출력한다. 즉, `th:if`의 편의 버전이다.

- #### `th:errorclass` 는 `th:field` 에서 지정한 필드에 오류가 있으면 `class` 정보를 추가하는 것이다.



### BindingResult V2



- #### 사용자가 입력한 값을 다시 볼 수 있게 한다.

- #### BindingResult는 검증 오류를 보관하는 것으로 ModelAttribute에 데이터 바인딩 시 오류가 발생해도 컨트롤러가 호출된다.

- #### 바인딩 시 타입 오류 발생 시 BindingResult가 중요 역할을 한다.

  - #### BindingResult 없으면 400 오류 발생하면서 컨트롤러가 호출되지 않고 오류페이지로 이동한다.

  - #### BindingResult 있으면 오류 정보(Field Error)를 BindingResult 에 담아서 컨트롤러가 정상 호출한다.

- #### BindingResult 검증 오류 적용 3가지 방법이 있다.

  - #### 바인딩 실패 시 스프링이 FieldError 생성하여 BindingResult에 넣는다. 자동으로 스프링이 한다.

  - #### 직접 검증 로직으로 넣어준다.

  - #### Validator을 사용한다.

- #### BindingResult는 Model에 자동으로 포함된다.

- #### BindingResult 인터페이스는 Errors 인터페이스를 상속받고 있다. 실제 넘어오는 구현체는 BeanPropertyBindingResult인데 BindingResult와 Errors 모두 구현하고 있다. Errors 인터페이스는 단순한 오류 저장과 조회 기능을 제공하며 BindingResult는 추가 기능을 더 제공한다.



```java
@PostMapping("/add")
public String addItemV2(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

    // 검증 로직
    // 하나하나 검증하는 로직
    if(!StringUtils.hasText(item.getItemName())){
        bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, null, null, "상품 이름은 필수 입니다."));
    }
    if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000){
        bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, null, null, "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
    }
    if(item.getQuantity() == null || item.getQuantity() >= 9999){
        bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, null, null, "수량은 최대 9,999 까지 허용합니다."));
    }

    // 특정 필드가 아닌 복합 룰 검증
    // 가격 * 수량의 합은 10,000 원 이상직
    if (item.getPrice() != null && item.getQuantity() != null){
        int resultPrice = item.getPrice() * item.getQuantity();
        if(resultPrice < 10000){
            bindingResult.addError(new ObjectError("item", null, null, "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
        }
    }

    // 검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()){
        log.info("errors = {}", bindingResult);
        return "validation/v2/addForm";
    }

    // 성공 로직
    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v2/items/{itemId}";
}
```

- #### `FieldError`의 3번째 매개변수는 rejectedValue로 거절된 값으로 사용자가 입력한 값이다.

- #### `FieldError`의 4번째 매개변수는 bindingFailure로 타입 오류같은 바인딩 실패인지 검증 실패인지 구분한다. 잘 들어오긴 했으면 false이다.

- #### `FieldError`의 5, 6번째 매개변수는 code, argument로 마지막 매개변수인 defaultMessage를 대체할 때 사용한다.

- #### `ObjectError` 는 필드가 아니므로 바인딩이 이미 되었다는 뜻으로 `FieldError`의 3, 4번째 매개변수가 없다.

- #### 바인딩 오류 시 사용자 값을 저장해야 하기 때문에 Model이 아닌 BindingResult에 저장한다. 바인딩 에러이므로 스프링이 자동으로 bindingFailure true, rejectedValue에 에러값을 넣어준다.



```html
th:field="*{price}"
```

- #### `th:field`는 정상 상황에는 모델 객체의 값을 사용하지만 오류 발생 시 `FieldError`에서 보관한 값을 사용해서 출력한다. 즉, rejectedValue 값을 이용한다.



### Error Code & Message



#### V1



```properties
# errors.properties
#required.item.itemName=상품 이름은 필수입니다.
#range.item.price=가격은 {0} ~ {1} 까지 허용합니다.
#max.item.quantity=수량은 최대 {0} 까지 허용합니다.
#totalPriceMin=가격 * 수량의 합은 {0}원 이상이어야 합니다. 현재 값 = {1}

#==ObjectError==
#Level1
totalPriceMin.item=상품의 가격 * 수량의 합은 {0}원 이상이어야 합니다. 현재 값 = {1}

#Level2 - 생략
totalPriceMin=전체 가격은 {0}원 이상이어야 합니다. 현재 값 = {1}

#==FieldError==
#Level1
required.item.itemName=상품 이름은 필수입니다.
range.item.price=가격은 {0} ~ {1} 까지 허용합니다.
max.item.quantity=수량은 최대 {0} 까지 허용합니다.

#Level2 - 생략

#Level3
required.java.lang.String = 필수 문자입니다.
required.java.lang.Integer = 필수 숫자입니다.
min.java.lang.String = {0} 이상의 문자를 입력해주세요.
min.java.lang.Integer = {0} 이상의 숫자를 입력해주세요.
range.java.lang.String = {0} ~ {1} 까지의 문자를 입력해주세요.
range.java.lang.Integer = {0} ~ {1} 까지의 숫자를 입력해주세요.
max.java.lang.String = {0} 까지의 문자를 허용합니다.
max.java.lang.Integer = {0} 까지의 숫자를 허용합니다.

#Level4
required = 필수 값 입니다.
min= {0} 이상이어야 합니다.
range= {0} ~ {1} 범위를 허용합니다.
max= {0} 까지 허용합니다.

# 추가
typeMismatch.java.lang.Integer=숫자를 입력해주세요.
typeMismatch=타입 오류입니다.
```

- #### `errorCode`, `arguments`로 오류 메시지 코드를 이용할 수 있다.

- #### `error.properties`에 오류 메시지를 저장하며 국제화 처리 가능하다.

- #### 에러 내용은 `요구사항(제약조건명).객체명.필드이름` 형식으로 저장한다.



```java
@PostMapping("/add")
public String addItemV3(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

    log.info("objectName={}", bindingResult.getObjectName());
    log.info("target={}", bindingResult.getTarget());

    // 검증 로직
    // 하나하나 검증하는 로직
    if(!StringUtils.hasText(item.getItemName())){
        bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, new String[]{"required.item.itemName"}, null, null));
    }
    if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000){
        bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, new String[]{"range.item.price"}, new Object[]{1000, 1000000}, null));
    }
    if(item.getQuantity() == null || item.getQuantity() >= 9999){
        bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, new String[]{"max.item.quantity"}, new Object[]{9999}, null));
    }

    // 특정 필드가 아닌 복합 룰 검증
    // 가격 * 수량의 합은 10,000 원 이상직
    if (item.getPrice() != null && item.getQuantity() != null){
        int resultPrice = item.getPrice() * item.getQuantity();
        if(resultPrice < 10000){
            bindingResult.addError(new ObjectError("item", new String[]{"totalPriceMin"}, new Object[]{10000, resultPrice}, null));
        }
    }

    // 검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()){
        log.info("errors = {}", bindingResult);
        return "validation/v2/addForm";
    }

    // 성공 로직
    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v2/items/{itemId}";
}
```

- #### `errorCode`는 0 번째 인자부터 검사하여 있는 것을 출력하려고 배열을 지원한다.

- #### `errorCode`로 사용할 에러를 탐지하고 `Message`로 넣을 대체할 인자를 설정할 수 있다.



#### V2



- #### 컨트롤럴에서 BindingResult는 검증해야 할 객체인 `target` 바로 다음에 오기 때문에 BindingResult는 이미 본인이 검증해야 할 객체인 `target`을 알고 있다.

- #### BindingResult가 제공하는 `rejectValue()`, `reject()`를 사용하여 `FieldError`,`ObjectError`를 직접 생성하지 않고 검증 오류를 다룰  수 있다.



```java
@PostMapping("/add")
public String addItemV4(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

    log.info("objectName={}", bindingResult.getObjectName());
    log.info("target={}", bindingResult.getTarget());

    // 검증에 실패하면 다시 입력 폼으로
    // 타입 오류 메시지만 출력
    /*if (bindingResult.hasErrors()){
            log.info("errors = {}", bindingResult);
            return "validation/v2/addForm";
        }*/

    // 검증 로직
    // 하나하나 검증하는 로직
    //        ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "itemName", "required")

    if(!StringUtils.hasText(item.getItemName())){
        bindingResult.rejectValue("itemName", "required");
    }
    if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000){
        bindingResult.rejectValue("price", "range", new Object[]{1000, 1000000}, null);
    }
    if(item.getQuantity() == null || item.getQuantity() >= 9999){
        bindingResult.rejectValue("quantity", "max", new Object[]{9999}, null);
    }

    // 특정 필드가 아닌 복합 룰 검증
    // 가격 * 수량의 합은 10,000 원 이상직
    if (item.getPrice() != null && item.getQuantity() != null){
        int resultPrice = item.getPrice() * item.getQuantity();
        if(resultPrice < 10000){
            bindingResult.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
        }
    }

    // 검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()){
        log.info("errors = {}", bindingResult);
        return "validation/v2/addForm";
    }

    // 성공 로직
    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v2/items/{itemId}";
}
```

- #### `rejectValue()`, `reject()`는 순서대로 필드명, properties내부 경로, argument, defaultValue 매개변수를 가질 수 있다.

- #### 이미 `target`을 알고 있어서 객체면에 대한 정보는 필요없다.

- #### Properties에 세밀한 메시지 코드일수록 높은 우선 순위를 가지게 된다. 이러한 방법은 MessageCodesResolver라는 것으로 지원된다.

- #### ObjectError

  ```
  객체 오류의 경우 다음 순서로 2가지 생성
  	1.: code + "." + object name
  	2.: code
  예) 오류 코드: required, object name: item
  	1.: required.item
  	2.: required
  ```

- #### FieldError

  ```
  필드 오류의 경우 다음 순서로 4가지 메시지 코드 생성
      1.: code + "." + object name + "." + field
      2.: code + "." + field
      3.: code + "." + field type
      4.: code
  예) 오류 코드: typeMismatch, object name "user", field "age", field type: int
      1. "typeMismatch.user.age"
      2. "typeMismatch.age"
      3. "typeMismatch.int"
      4. "typeMismatch"
  ```

- #### 위 내용처럼 MessageCodesResolver은 생성된 여러 오류 코드를 보관하며 사용 시 순서에 따라 제공한다.

- #### RejectValue()는 내부적으로 자동으로 MessageCodesResolver을 사용한다. 그래서 rejectValue()에 fieldName, errorCode를 넣는 것이다. 이후에 FieldError을 생성하여 자동으로 errorCode의 messageCode를 넣는다.

- #### 타임리프 렌더링 시 `th:errors`가 실행되며 오류가 있다면 생성된 오류 메시지 코드를 순서대로 돌아가면서 메시지를 찾는다. 없으면 default 메시지 출력한다.



```java
ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "itemName", "required");
```

- #### 검증 로직으로 null값인지 공백인지 확인하는 것이다.

- #### 검증 오류 코드는 2가지로 나눌 수 있다.

  - #### 개발자가 직접 설정한 오류 코드로 rejectValue() 직접 호출한다.

  - #### 스프링이 직접 자동으로 검증 오류에 추가한 경우로 주로 타입 정보가 맞지 않는 경우이다.

- #### 스프링은 타입 오류가 발생하면 typeMismatch라는 오류 코드를 사용한다. 이 코드가 MessageCodesResolver를 통하면서 4가지 메시지 코드가 생성된 것이다.

  ```properties
  typeMismatch.java.lang.Integer=숫자를 입력해주세요.
  typeMismatch=타입 오류입니다.
  ```

  - #### `error.properties`에 추가하여 스프링이 자동으로 오류 코드를 검색하여 처리하게 할 수 있다.

  ```java
  if (bindingResult.hasErrors()){
      log.info("errors = {}", bindingResult);
      return "validation/v2/addForm";
  }
  ```

  - #### 타입 에러 시 바로 처리하게 하는 코드를 컨트롤러 가장 앞에 설정하여 타입 오류만 우선 처리하게 할 수 있다.



## Validator



#### V1



- #### Validator 검증기를 직접 불러서 사용한다.

```java
@Component
public class ItemValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Item.class.isAssignableFrom(clazz);
        // item == clazz
        // item == subItem
    }

    @Override
    public void validate(Object target, Errors errors) {
        Item item = (Item) target;

        if(!StringUtils.hasText(item.getItemName())){
            errors.rejectValue("itemName", "required");
        }
        if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000){
            errors.rejectValue("price", "range", new Object[]{1000, 1000000}, null);
        }
        if(item.getQuantity() == null || item.getQuantity() >= 9999){
            errors.rejectValue("quantity", "max", new Object[]{9999}, null);
        }

        // 특정 필드가 아닌 복합 룰 검증
        // 가격 * 수량의 합은 10,000 원 이상직
        if (item.getPrice() != null && item.getQuantity() != null){
            int resultPrice = item.getPrice() * item.getQuantity();
            if(resultPrice < 10000){
                errors.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
            }
        }
    }
}
```

```java
@PostMapping("/add")
public String addItemV5(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

    // 검증 로직 클래스 validator
    itemValidator.validate(item, bindingResult);

    // 검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()){
        log.info("errors = {}", bindingResult);
        return "validation/v2/addForm";
    }

    // 성공 로직
    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v2/items/{itemId}";
}
```

- #### 검증 로직을 분리한 것으로 재사용성을 고려한 것이다.

- #### `supports()`는 해당 검증기를 지원하는 여부를 확인하는 것이다. 즉, 파라미터로 들어오는 클래스가 해당 검증하려는 클래스를 지원하는지 확인하는 것으로 자식 클래스도 검증이 허용된다.

- #### `validate()`는 검증 대상 객체와 BindingResult가 매개변수로 들어가서 검증을 실행하는 것이다.



#### V2



- #### Validator 인터페이스를 사용하여 검증기를 만들어서 스프링의 자동화 기능을 사용한다.

```java
@InitBinder
public void init(WebDataBinder dataBinder){
    dataBinder.addValidators(itemValidator);
}

@PostMapping("/add")
public String addItemV6(@Validated @ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

    // 검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()){
        log.info("errors = {}", bindingResult);
        return "validation/v2/addForm";
    }

    // 성공 로직
    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v2/items/{itemId}";
}
```

- #### `WebDataBinder`에 검증기를 추가하여 해당 컨트롤러에서 검증기를 자동으로 적용할 수 있다. `WebDataBinder`에 이전에 직접 생성한 `ItemValidator` 클래스를 등록하여 사용한다.

- #### Validator 직접 호출 부분이 사라지고 검증 대상 앞에 @Validated를 붙인다.

- #### @Validated는 검증기를 실행하라는 어노테이션이며 `supports()`로 어떤 검증기를 실행할지 탐지한다. 탐지 후 `validate()`가 호출된다.

- #### @Validated, @Valid 둘다 사용 가능하다. @Validated는 스프링 전용 검증 어노테이션이며 @Valid는 자바 표준 검증 어노테이션이다. @Valid 사용 시 gradle에 의존관계를 추가해야 한다.



```java
@SpringBootApplication
public class ItemServiceApplication implements WebMvcConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(ItemServiceApplication.class, args);
	}

	@Override
	public Validator getValidator(){
		return new ItemValidator();
	}
}
```

- #### Validator 글로벌 설정으로 모든 컨트롤러에서 해당 validator로 검증할 수 있다.

- #### 글로벌 설정 시 BeanValidator이 작동하지 않는 것을 주의해야 한다.



# Bean Validation



- #### Bean Validation은 특정 구현체가 아닌 Bean Validation 2.0(JSR-380)이라는 기술 표준이디ㅏ. 즉, 검증 어노테이션과 여러 인터페이스의 모음이다.

- #### Bean Validation 구현한 기술 중 일반적으로 사용하는 구현체는 하이버네이트 Validator이다.



```groovy
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

- #### Bean Validation을 사용하려면 gradle에 추가해야 한다.

- #### `@NotBlank` 는 빈값 + 공백만 있는 경우를 허용하지 않는다.

  - #### `@NotBlank(message = "공백X")`로 기본 오류 메시지 변경 가능하다.

- #### `@NotNull` 은 null을 허용하지 않는다.

- #### `@Range(min = 1000, max = 1000000)` 는 범위 안의 값이어야 한다.

- #### `@Max(9999)` 는 최대 9999까지만 허용한다.

- #### `@NotNull`, `@NotEmpty`, `@NotBlank`는 서로 다르다.

  - #### `@NotNull`은 null만 허용하지 않으며 "", " "은 허용한다.

  - #### `@NotEmpty`는 null, ""를 허용하지 않으며 " "은 허용한다.

  - #### `@NotBlank`는 null, "", " " 모두 허용하지 않는다.

- #### 빈 사용 시 javax.validation으로 시작하면 특정 구현에 관계없이 제공되는 표준 인터페이스이고 org.hibernate.validator로 시작하면 하이버네이트 validator 구현체를 사용할 때만 제공되는 검증 기능이다. 실무에서 대부분 하이버네이트 validator을 사용한다.

- #### 스프링은 자동으로 LocalValidatorFactoryBean을 글로벌 Validator로 등록한다. 이 validator이 어노테이션 기반을 보고 검증을 수행한다. 글로벌 validator 덕분에 @Valid, @Validated만 적용하면 된다. 검증 오류가 발생하면 FieldError, ObjectError를 생성행서 BindingResult에 담아준다.

- #### 검증 순서는 2 단계로 나누어진다.

  - #### @ModelAttribute 각각의 필드에 타입 변환을 시도한다.

    - #### 성공하면 다음으로

    - #### 실패하면 typeMismatch로 FieldError 추가

  - #### Validator 적용

- #### Bean Validator은 바인딩에 실패한 필드는 적용하지 않는다.



## Error Code



```properties
# errors.properties
# Bean Validation 추가
NotBlank={0} 공백X
Range={0}, {2} ~ {1} 허용
Max={0}, 최대 {1}
```

- #### `error.properties`에 빈 어노테이션 기반의 오류 코드를 생성하여 validator을 사용한다.

- #### {0}은 필드명이고 {1}, {2}, ... 은 각 어노테이션마다 다르다.

- #### Bean Validation 순서는 3 단계로 나누어진다.

  - #### 생성된 메시지 코드 순서대로 messageSource에서 메서지 찾는다.

  - #### 어노테이션 message 속성을 사용한다.

    `@NotBlank(message = "공백! {0}")`

  - #### 라이브러리가 제공하는 기본 값 사용한다.

    `공백일 수 없습니다.`



## Object Error



```java
@Data
@ScriptAssert(lang = "javascript", script = "_this.price * _this.quantity >= 10000", message = "총합이 10000원 넘게 입력해주세요.")
```

- #### @ScriptAssert로 Object Error을 처리하는 것이 가능하다.

- #### 생성되는 메시지 코드는 2가지이다.

  - #### ScriptAssert.item

  - #### ScriptAssert



```java
if (item.getPrice() != null && item.getQuantity() != null) {
    int resultPrice = item.getPrice() * item.getQuantity();
    if (resultPrice < 10000) {
        bindingResult.reject("totalPriceMin", new Object[]{10000,
                                                           resultPrice}, null);
    }
}
```

- #### @ScriptAssert는 제약이 많고 복잡하다. 즉, 실무에서는 검증 기능이 해당 객체의 범위를 넘어서는 경우들도 종종 발생한다.

- #### Object Error은 컨트롤러에서 로직을 추가하는 것이 편하고 좋다. 이후 따로 메소드로 만드는 것이 재사용성에 좋다.



```java
@PostMapping("/{itemId}/edit")
public String editV1(@PathVariable Long itemId, @Validated @ModelAttribute Item item, BindingResult bindingResult) {

    if (item.getPrice() != null && item.getQuantity() != null) {
        int resultPrice = item.getPrice() * item.getQuantity();
        if (resultPrice < 10000) {
            bindingResult.reject("totalPriceMin", new Object[]{10000,
                                                               resultPrice}, null);
        }
    }

    if(bindingResult.hasErrors()){
        log.info("errors = {}", bindingResult);
        return "validation/v3/editForm";
    }

    itemRepository.update(itemId, item);
    return "redirect:/validation/v3/items/{itemId}";
}
```

- #### 수정 컨트롤러는 타입 오류 검증, 필드 에러 검증, 글로벌 에러 검증 순서로 검증을 거치게 된다.

- #### 현재 한 클래스로 bean validation을 이용 중이지만 각 로직마다 요구사항이 다를 수 있다.

  - #### 등록 시에는 `id`가 필수가 아니지만 수정 시에는 `id`가 필수여야 한다.

  - #### 이때 각 로직을 모두 만족하지 않아서 400 상태 오류가 발생할 수 있다.



## Groups



- #### 동일한 모델 객체를 등록할 때와 수정할 때 각각 다르게 검증하는 2가지 방법이 있다.

  - #### Bean Validation의 groups 기능을 사용한다.

  - #### Item 클래스를 직접 사용하지 않고, ItemSaveForm, ItemUpdateForm 같은 폼 전송을 위한 별도의 모델 객체를 만들어서 사용한다.



```java
public interface SaveCheck {
}
```

```java
public interface UpdateCheck {
}
```

```java
@Data
public class Item {

    @NotNull(groups = UpdateCheck.class) // 수정 요구사항 추가
    private Long id;

    @NotBlank(groups = {SaveCheck.class, UpdateCheck.class})
    private String itemName;

    @NotNull(groups = {SaveCheck.class, UpdateCheck.class})
    @Range(min = 1000, max = 1000000, groups = {SaveCheck.class, UpdateCheck.class})
    private Integer price;

	@NotNull(groups = {SaveCheck.class, UpdateCheck.class})
	@Max(value = 9999, groups = SaveCheck.class) // 수정 요구사항 추가
    private Integer quantity;

    public Item() {
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}
```

```java
// ValidationItemControllerV3
@PostMapping("/add")
public String addItemV2(@Validated(SaveCheck.class) @ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
}
```

```java
// ValidationItemControllerV3
@PostMapping("/{itemId}/edit")
public String editV2(@PathVariable Long itemId, @Validated(UpdateCheck.class) @ModelAttribute Item item, BindingResult bindingResult) {
}
```

- #### @Valid에는 groups를 적용할 수 있는 기능이 없으므로 @Validated를 사용한다.

- #### Groups로 로직마다 사용하는 validation을 설정하였다.

- #### Groups는 인터페이스로 구현 후 적용한다.

- #### 사실 Groups는 실제 잘 사용안하며 실무에서는 주로 등록용 폼 객체와 수정용 폼 객체를 분리하여 사용한다. 등록, 수정은 보통 객체 구조가 많이 달라서 객체를 따로 생성한다.



## Object Separation Form



- #### 폼 데이터 전달에 Item 도메인 객체 사용

  - #### HTML Form -> Item -> Controller -> Item -> Repository

    - #### 장점 : Item 도메인 객체를 컨트롤러, 리포지토리까지 직접 전달해서 중간에 Item을 만드는 과정이 없어서 간단하다.

    - #### 단점 : 간단한 경우에만 적용할 수 있다. 수정 시 검증이 중복될 수 있고 groups를 사용해야 한다.

- #### 폼 데이터 전달을 위한 별도의 객체 사용

  - #### HTML Form -> ItemSaveForm -> Controller -> Item 생성 -> Repository

    - #### 장점 : 전송하는 폼 데이터가 복잡해도 거기에 맞춘 별도의 폼 객체를 사용해서 데이터를 전달 받을 수 있다. 보통 등록과 수정용으로 별도의 폼 객체를 만들기 때문에 검증이 중복되지 않는다.

    - #### 단점 : 폼 데이터를 기반으로 컨트롤러에서 Item 객체를 생성하는 변환 과정이 추가된다.

- #### 별도의 객체에 이름 짓기

  - #### ItemSaveForm(Form), ItemSaveRequest(API), ItemSaveDto 등 사용하여 일관성 있게 이름을 부여한다.

- #### 등록, 수정용 뷰 템플릿은 합치지말고 나눠야 유지보수에 좋다.



```java
@Data
public class ItemSaveForm {

    @NotBlank
    private String itemName;

    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;

    @NotNull
    @Max(value = 9999)
    private Integer quantity;

}
```

```java
@Data
public class ItemUpdateForm {

    @NotNull // 수정 요구사항 추가
    private Long id;

    @NotBlank
    private String itemName;

    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;

    // 수정에서는 수량 자유롭게 변경 가능 (수량 무제한, null 가능)
    private Integer quantity;

}
```

```java
@PostMapping("/add")
public String addItem(@Validated @ModelAttribute("item") ItemSaveForm form, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

    // 특정 필드가 아닌 복합 룰 검증
    if (form.getPrice() != null && form.getQuantity() != null) {
        int resultPrice = form.getPrice() * form.getQuantity();
        if (resultPrice < 10000) {
            bindingResult.reject("totalPriceMin", new Object[]{10000,
                                                               resultPrice}, null);
        }
    }

    // 검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()){
        log.info("errors = {}", bindingResult);
        return "validation/v4/addForm";
    }

    // 성공 로직

    Item item = new Item();
    item.setItemName(form.getItemName());
    item.setPrice(form.getPrice());
    item.setQuantity(form.getQuantity());

    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v4/items/{itemId}";
}
```

- #### Item 객체는 검증에 사용하지 않으므로 Bean Validation 어노테이션을 제거한다.

- #### @ModelAttribute("item")으로 해야 뷰 템플릿을 수정하지 않아도 된다.

- #### ItemSaveForm 객체로 데이터를 받은 뒤  validation 후 Item객체를 생성하여 넣어준다. 이 후 저장한다. 이때 Item 객체에 생성자로 값을 넣어주는 것이 좋다.

- #### ValidationItemControllerV4 방식을 주로 이용한다.



## HTTP Message Converter



```java
@RestController
@RequestMapping("/validation/api/items")
public class ValidationItemApiController {

    @PostMapping("/add")
    public Object addItem(@RequestBody @Validated ItemSaveForm form, BindingResult bindingResult){

        log.info("API 컨트롤러 호출");

        if(bindingResult.hasErrors()){
            log.info("검증 오류 발생 errors = {}", bindingResult);
            return bindingResult.getAllErrors();
        }

        log.info("성공 로직 실행");
        return form;
    }

}
```

- #### HTTP API 요청을 받아서 validation하는 것이다.

- #### 타입 에러시 400 상태 에러를 응답하며 message에는 아무것도 없다. 즉, JSON을 Item 객체로 만들지 못한 것이며 컨트롤러도 호출되지 않는다.

- #### `getAllErrors()`는 Object Error, Field Error의 모든 정보를 반환한다. 실제 사용할 때에는 필요한 정보만 사용한다.

- #### 위 코드에는 Object Error 코드는 없다.



### @ModelAttribute vs @RequestBody

- #### @ModelAttribute는 쿼리 파라미터를 처리하며 @RequestBody는 JSON을 처리한다.

- #### HTTP 요청 파라미터를 처리하는 @ModelAttribute는 각각의 필드 단위로 세밀하게 적용된다. 그래서 특정 필드에 타입이 맞지 않는 오류가 발생해도 나머지 필드는 정상 처리할 수 있다.

- #### HttpMessageConverter는 @ModelAttribute와 다르게 각각의 필드 단위로 적용되는 것이 아니라 전체 객체 단위로 적용된다. 따라서 메시지 컨버터의 작동이 성공해서 Item 객체를 만들어야 Validation이 실행된다.

- #### @ModelAttribute는 필드 단위로 정교하게 바인딩이 적용된다. 특정 필드가 바인딩 되지 않아도 나머지 필드는 정상 바인딩 되고 Validator를 사용한 검증도 적용할 수 있다.

- #### @RequestBody는 HttpMessageConverter 단계에서 JSON 데이터를 객체로 변경하지 못하면 이후 단계 자체가 진행되지 않고 예외가 발생한다. 즉, 컨트롤러도 호출되지 않고 Validator를 실행할 수도 없다.
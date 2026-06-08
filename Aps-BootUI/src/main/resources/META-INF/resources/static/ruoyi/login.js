
// AES加密配置，与VUEUI的aes.js保持一致
var AES_KEY = 'zlt_aps_pwd_key!';

/**
 * AES-ECB加密，使用CryptoJS
 * 与VUEUI的src/utils/aes.js加密逻辑一致，确保前后端解密兼容
 * @param {string} text 待加密的文本
 * @returns {string} 加密后的Base64字符串
 */
function aesEncrypt(text) {
    if (!text) return text;
    var key = CryptoJS.enc.Utf8.parse(AES_KEY);
    var encrypted = CryptoJS.AES.encrypt(text, key, {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
    });
    return encrypted.toString();
}

// 动态加载CryptoJS库，加载完成后再初始化登录逻辑
function loadCryptoJS(callback) {
    if (typeof CryptoJS !== 'undefined') {
        callback();
        return;
    }
    var script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/crypto-js@4.2.0/crypto-js.min.js';
    script.onload = callback;
    script.onerror = function() {
        // CDN加载失败时，尝试本地路径
        var localScript = document.createElement('script');
        localScript.src = ctx + 'ruoyi/crypto-js.min.js';
        localScript.onload = callback;
        localScript.onerror = function() {
            console.error('CryptoJS加载失败，密码将不进行AES加密');
            callback();
        };
        document.head.appendChild(localScript);
    };
    document.head.appendChild(script);
}

$(function() {
    loadCryptoJS(function() {
        validateKickout();
        validateRule();
        $('.imgcode').click(function() {
            var url = ctx + "captcha/captchaImage?type=" + captchaType + "&s=" + Math.random();
            $(".imgcode").attr("src", url);
        });
    });
});

$.validator.setDefaults({
    submitHandler: function() {
        login();
    }
});

function login() {
    $.modal.loading($("#btnSubmit").data("loading"));
    var username = $.common.trim($("input[name='username']").val());
    var password = $.common.trim($("input[name='password']").val());
    var validateCode = $("input[name='validateCode']").val();
    var rememberMe = $("input[name='rememberme']").is(':checked');

    // 对密码进行AES加密，与VUEUI登录逻辑保持一致
    debugger
    var encryptedPassword = (typeof CryptoJS !== 'undefined') ? aesEncrypt(password) : password;

    $.ajax({
        type: "post",
        url: ctx + "login",
        data: {
            "username": username,
            "password": encryptedPassword,
            "validateCode": validateCode,
            "rememberMe": rememberMe,
            "lang": ui_locale_server
        },
        success: function(r) {
            if (r.code == web_status.SUCCESS) {
                window.self.location = ctx + 'index';
            } else {
            	$('.imgcode').click();
            	$(".code").val("");
            	$.modal.msg(r.msg);
            }
            $.modal.closeLoading();
        }
    });
}

function validateRule() {
    var icon = "<i class='fa fa-times-circle'></i> ";
    $("#signupForm").validate({
        rules: {
            username: {
                required: true
            },
            password: {
                required: true
            }
        },
        messages: {
            username: {
                required: icon + frame.login.usernameRequired,
            },
            password: {
                required: icon + frame.login.passwordRequired,
            }
        }
    })
}

function validateKickout() {
    if (getParam("kickout") == 1) {
        layer.alert("<font color='red'>" + frame.alter.kickout + "</font>", {
            icon: 0,
            title: frame.title.sysAlter
        },
        function(index) {
            //关闭弹窗
            layer.close(index);
            if (top != self) {
                top.location = self.location;
            } else {
                var url = location.search;
                if (url) {
                    var oldUrl = window.location.href;
                    var newUrl = oldUrl.substring(0, oldUrl.indexOf('?'));
                    self.location = newUrl;
                }
            }
        });
    }
}

function getParam(paramName) {
    var reg = new RegExp("(^|&)" + paramName + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURI(r[2]);
    return null;
}
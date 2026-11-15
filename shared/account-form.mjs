export function validateAccountForm(form, settingPassword = false) {
  if (!/^[\u3400-\u9fffA-Za-z0-9_.-]{2,32}$/.test(form.username || '')) return '账号名需为2–32位中文、英文字母、数字或_.-'
  if (!form.password) return '请输入密码'
  if (form.password.length < 12 || form.password.length > 128) return '密码需为12–128位'
  if (settingPassword && !form.confirmPassword) return '请再次输入密码'
  if (settingPassword && form.password !== form.confirmPassword) return '两次输入的密码不一致'
  return ''
}

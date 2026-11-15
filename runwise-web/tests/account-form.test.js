import {test} from 'node:test'
import assert from 'node:assert/strict'
import {validateAccountForm} from '../../shared/account-form.mjs'
test('Chinese and existing Latin account names are accepted by the shared form policy',()=>{
 for(const username of ['小明','跑者小明','NakiTong','runner_01'])assert.equal(validateAccountForm({username,password:'test-password-123'}),'')
})
test('setting a password requires a matching second input; login does not',()=>{
 const form={username:'小明',password:'test-password-123'}
 assert.match(validateAccountForm(form,true),/再次/)
 assert.match(validateAccountForm({...form,confirmPassword:'wrong'},true),/不一致/)
 assert.equal(validateAccountForm({...form,confirmPassword:form.password},true),'')
 assert.equal(validateAccountForm(form,false),'')
 assert.notEqual(validateAccountForm({...form,username:'bad name'}),'')
})

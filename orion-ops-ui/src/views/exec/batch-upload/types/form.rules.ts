import type { FieldRule } from '@arco-design/web-vue';

export const description = [{
  maxLength: 128,
  message: '描述长度不能大于128位'
}] as FieldRule[];

export const hostIdList = [{
  required: true,
  message: '请选择上传主机'
}] as FieldRule[];

export const remotePath = [{
  required: true,
  message: '请输入上传路径'
}, {
  maxLength: 1024,
  message: '上传路径长度不能大于1024位'
}] as FieldRule[];

export const files = [{
  required: true,
  message: '请选择文件'
}] as FieldRule[];

export default {
  description,
  hostIdList,
  remotePath,
  files,
} as Record<string, FieldRule | FieldRule[]>;

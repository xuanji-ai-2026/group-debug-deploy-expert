<template>
  <!--
    BxInput 组件 - 北极星输入框组件
    基于 Element Plus el-input 封装的业务输入框组件
  -->
  <div class="bx-input-wrapper" :class="{ 'is-focused': isFocused, 'has-error': hasError }">
    <!-- 标签 -->
    <label v-if="label" class="bx-input__label" :for="inputId">
      {{ label }}
      <span v-if="required" class="bx-input__required">*</span>
    </label>

    <!-- 输入框组 -->
    <div class="bx-input__group">
      <!-- 前置插槽 -->
      <span v-if="$slots.prepend" class="bx-input__prepend">
        <slot name="prepend" />
      </span>

      <!-- 输入框 -->
      <el-input
        :id="inputId"
        ref="inputRef"
        v-model="inputValue"
        :type="type"
        :size="size"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :clearable="clearable"
        :show-password="showPassword"
        :prefix-icon="prefixIcon"
        :suffix-icon="computedSuffixIcon"
        :maxlength="maxlength"
        :minlength="minlength"
        :rows="rows"
        :autosize="autosize"
        :resize="resize"
        :name="name"
        :form="form"
        :autocomplete="autocomplete"
        :autofocus="autofocus"
        :step="step"
        :tabindex="tabindex"
        :validate-event="validateEvent"
        :class="[
          'bx-input',
          `bx-input--${size}`,
          {
            'is-prepend': $slots.prepend,
            'is-append': $slots.append,
            'is-prefix': prefixIcon || $slots.prefix,
            'is-suffix': suffixIcon || $slots.suffix || showPassword || clearable,
          },
        ]"
        @focus="handleFocus"
        @blur="handleBlur"
        @input="handleInput"
        @change="handleChange"
        @clear="handleClear"
      >
        <!-- 前缀插槽 -->
        <template v-if="$slots.prefix" #prefix>
          <slot name="prefix" />
        </template>
        <!-- 后缀插槽 -->
        <template v-if="$slots.suffix" #suffix>
          <slot name="suffix" />
        </template>
        <!-- 头部插槽 -->
        <template v-if="$slots.append" #append>
          <slot name="append" />
        </template>
      </el-input>
    </div>

    <!-- 错误提示 -->
    <transition name="el-zoom-in-top">
      <p v-if="hasError && errorMessage" class="bx-input__error">
        <i class="bx-input__error-icon el-icon-warning" />
        {{ errorMessage }}
      </p>
    </transition>

    <!-- 帮助文本 -->
    <p v-else-if="helpText" class="bx-input__help">
      {{ helpText }}
    </p>

    <!-- 字符计数 -->
    <p v-if="showWordLimit && maxlength" class="bx-input__count">
      {{ inputValue?.length || 0 }} / {{ maxlength }}
    </p>
  </div>
</template>

<script setup>
/**
 * @fileoverview BxInput 输入框组件
 * @description 统一的输入框封装，支持标签、验证、图标等功能
 * @author EMP-FE-001 张婷
 */
import { ref, computed, watch, useSlots } from 'vue'

// ============================================================
// Props
// ============================================================
const props = defineProps({
  /**
   * 绑定值
   */
  modelValue: {
    type: [String, Number],
    default: '',
  },

  /**
   * 输入框类型
   */
  type: {
    type: String,
    default: 'text',
  },

  /**
   * 输入框尺寸
   */
  size: {
    type: String,
    default: 'default',
    validator: (val) => ['large', 'default', 'small'].includes(val),
  },

  /**
   * 标签文本
   */
  label: {
    type: String,
    default: '',
  },

  /**
   * 占位符
   */
  placeholder: {
    type: String,
    default: '',
  },

  /**
   * 是否禁用
   */
  disabled: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否只读
   */
  readonly: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否可清空
   */
  clearable: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示密码切换
   */
  showPassword: {
    type: Boolean,
    default: false,
  },

  /**
   * 前缀图标
   */
  prefixIcon: {
    type: String,
    default: '',
  },

  /**
   * 后缀图标
   */
  suffixIcon: {
    type: String,
    default: '',
  },

  /**
   * 最大长度
   */
  maxlength: {
    type: [String, Number],
    default: null,
  },

  /**
   * 最小长度
   */
  minlength: {
    type: [String, Number],
    default: null,
  },

  /**
   * 多行文本行数
   */
  rows: {
    type: Number,
    default: 3,
  },

  /**
   * textarea 自动高度
   */
  autosize: {
    type: [Boolean, Object],
    default: false,
  },

  /**
   * 文本域是否可调整大小
   */
  resize: {
    type: String,
    default: 'vertical',
  },

  /**
   * 原生 name 属性
   */
  name: {
    type: String,
    default: '',
  },

  /**
   * 原生 form 属性
   */
  form: {
    type: String,
    default: '',
  },

  /**
   * 自动完成
   */
  autocomplete: {
    type: String,
    default: 'off',
  },

  /**
   * 自动聚焦
   */
  autofocus: {
    type: Boolean,
    default: false,
  },

  /**
   * 数字输入步进
   */
  step: {
    type: [String, Number],
    default: null,
  },

  /**
   * Tab 索引
   */
  tabindex: {
    type: [String, Number],
    default: '',
  },

  /**
   * 是否触发表单验证
   */
  validateEvent: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否必填
   */
  required: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示错误
   */
  hasError: {
    type: Boolean,
    default: false,
  },

  /**
   * 错误信息
   */
  errorMessage: {
    type: String,
    default: '',
  },

  /**
   * 帮助文本
   */
  helpText: {
    type: String,
    default: '',
  },

  /**
   * 是否显示字数统计
   */
  showWordLimit: {
    type: Boolean,
    default: false,
  },

  /**
   * 输入框 ID
   */
  id: {
    type: String,
    default: '',
  },
})

// ============================================================
// Emits
// ============================================================
const emit = defineEmits([
  'update:modelValue',
  'focus',
  'blur',
  'input',
  'change',
  'clear',
])

// ============================================================
// 组件引用
// ============================================================
const inputRef = ref(null)
const slots = useSlots()

// ============================================================
// 状态
// ============================================================
const isFocused = ref(false)

// ============================================================
// 计算属性
// ============================================================

/**
 * 输入框 ID
 */
const inputId = computed(() => props.id || `bx-input-${Math.random().toString(36).slice(2, 9)}`)

/**
 * 双向绑定值
 */
const inputValue = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

/**
 * 计算后缀图标
 */
const computedSuffixIcon = computed(() => {
  if (props.suffixIcon) return props.suffixIcon
  return ''
})

// ============================================================
// 方法
// ============================================================

function handleFocus(event) {
  isFocused.value = true
  emit('focus', event)
}

function handleBlur(event) {
  isFocused.value = false
  emit('blur', event)
}

function handleInput(value) {
  emit('input', value)
}

function handleChange(value) {
  emit('change', value)
}

function handleClear() {
  emit('clear')
}

/**
 * 聚焦到输入框
 */
function focus() {
  inputRef.value?.focus()
}

/**
 * 失焦输入框
 */
function blur() {
  inputRef.value?.blur()
}

/**
 * 选中输入框内容
 */
function select() {
  inputRef.value?.select()
}

// ============================================================
// 暴露方法给父组件
// ============================================================
defineExpose({
  focus,
  blur,
  select,
  inputRef,
})
</script>

<style lang="scss" scoped>
.bx-input-wrapper {
  width: 100%;
}

.bx-input__label {
  display: block;
  margin-bottom: 6px;
  font-size: $font-size-sm;
  font-weight: $font-weight-medium;
  color: $color-text-primary;
  line-height: 1.5;

  .bx-input__required {
    color: $color-danger;
    margin-left: 2px;
  }
}

.bx-input__group {
  display: flex;
  width: 100%;
}

.bx-input__prepend {
  display: flex;
  align-items: center;
  padding: 0 12px;
  background-color: $color-gray-50;
  border: 1px solid $color-border-base;
  border-right: none;
  border-radius: $radius-md 0 0 $radius-md;
  color: $color-text-regular;
  font-size: $font-size-sm;
}

.bx-input__error {
  display: flex;
  align-items: center;
  margin: 6px 0 0;
  font-size: $font-size-xs;
  color: $color-danger;

  .bx-input__error-icon {
    margin-right: 4px;
  }
}

.bx-input__help {
  margin: 6px 0 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.bx-input__count {
  text-align: right;
  margin: 6px 0 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

// 聚焦样式
.has-error {
  .bx-input {
    :deep(.el-input__wrapper) {
      box-shadow: 0 0 0 1px $color-danger inset;
    }
  }
}
</style>

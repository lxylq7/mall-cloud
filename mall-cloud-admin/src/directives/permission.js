import { useUserStore } from '@/stores/user'
import { hasButtonPermission } from '@/utils/permission'

export default {
  install(app) {
    app.directive('permission', {
      mounted(el, binding) {
        const userStore = useUserStore()
        const has = hasButtonPermission(binding.value, userStore.permissions)
        if (!has) {
          el.parentNode?.removeChild(el)
        }
      },
    })
  },
}

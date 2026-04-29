import React, { useEffect, useMemo, useRef } from 'react'
import {
  type StyleProp,
  type ViewProps,
  type ViewStyle,
  View,
  findNodeHandle,
} from 'react-native'
import { NitroModules } from 'react-native-nitro-modules'
import type { NitroOrientation as NitroOrientationSpec } from './specs/NitroOrientation.nitro'
import type {
  ZoneRotationEvent,
  ZoneRotationSnapshot,
} from './orientation.types'

const NitroOrientation =
  NitroModules.createHybridObject<NitroOrientationSpec>('NitroOrientation')

export interface NitroZoneViewProps extends ViewProps {
  zoneId: string
  zoneOptions?: Record<string, unknown>
  destroyOnUnmount?: boolean
  style?: StyleProp<ViewStyle>
}

export function NitroZoneView({
  zoneId,
  zoneOptions,
  destroyOnUnmount = true,
  ...rest
}: NitroZoneViewProps) {
  const ref = useRef<View>(null)

  useEffect(() => {
    NitroOrientation.createZone(zoneId, JSON.stringify(zoneOptions ?? {}))
    const hostTag = findNodeHandle(ref.current)
    if (typeof hostTag === 'number') {
      NitroOrientation.registerZoneHost(zoneId, hostTag)
    }
    return () => {
      if (typeof hostTag === 'number') {
        NitroOrientation.unregisterZoneHost(zoneId, hostTag)
      }
      if (destroyOnUnmount) {
        NitroOrientation.destroyZone(zoneId)
      }
    }
  }, [zoneId, destroyOnUnmount, zoneOptions])

  return <View ref={ref} {...rest} />
}

export function useZoneRotation(zoneId: string) {
  const store = useMemo(() => {
    const getSnapshot = (): ZoneRotationSnapshot | null => {
      try {
        return JSON.parse(NitroOrientation.getZoneSnapshot(zoneId))
      } catch {
        return null
      }
    }
    const subscribe = (onStoreChange: () => void) => {
      NitroOrientation.setZoneListener((eventJson) => {
        try {
          const event = JSON.parse(eventJson) as ZoneRotationEvent
          if (event.snapshot.zoneId === zoneId) {
            onStoreChange()
          }
        } catch {
          // ignore malformed event payloads
        }
      })
      return () => NitroOrientation.setZoneListener(() => {})
    }
    return { subscribe, getSnapshot }
  }, [zoneId])
  return React.useSyncExternalStore(store.subscribe, store.getSnapshot)
}
